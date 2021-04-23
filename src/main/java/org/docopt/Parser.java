package org.docopt;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class Parser {

    static Required parsePattern(final String source,
                                 final List<Option> options) {
        String source1 = source;
        source1 = Py.Re.INSTANCE.sub("([\\[\\]\\(\\)\\|]|\\.\\.\\.)", " $1 ", source1);
        List<String> $source;
        {
            $source = Py.INSTANCE.list();


            for (final String s : Py.Re.INSTANCE.split("\\s+|(\\S*<.*?>)", source1)) {
                if (s != null && !s.equals("")) {
                    $source.add(s);
                }
            }
        }

        final Tokens tokens = new Tokens($source, DocoptLanguageError.class);
        final List<? extends Pattern> result = Helper.INSTANCE.parseExpr(tokens, options);

        if (tokens.current() != null) {
            throw tokens.error("unexpected ending: %s", Py.INSTANCE.join(" ", tokens));
        }

        return new Required(result);
    }

    static List<LeafPattern> parseArgv(final Tokens tokens,
                                       final List<Option> options, final boolean optionsFirst) {
        final List<LeafPattern> parsed = Py.INSTANCE.list();

        while (tokens.current() != null) {
            if ("--".equals(tokens.current())) {
                {
                    for (final String v : tokens) {
                        parsed.add(new Argument(null, v));
                    }

                    return parsed;
                }
            }

            // TODO: Why don't we check for tokens.current != "--" here?
            if (tokens.current().startsWith("--")) {
                parsed.addAll(Helper.INSTANCE.parseLong(tokens, options));
            } else if (tokens.current().startsWith("-")
                    && !"-".equals(tokens.current())) {
                parsed.addAll(Helper.INSTANCE.parseShorts(tokens, options));
            } else if (optionsFirst) {
                {
                    for (final String v : tokens) {
                        parsed.add(new Argument(null, v));
                    }

                    return parsed;
                }
            } else {
                parsed.add(new Argument(null, tokens.move()));
            }
        }

        return parsed;
    }

    static String read(final InputStream stream) {
        try (Scanner scanner = new Scanner(stream)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    static List<String> parseSection(final String name,
                                     final String source) {
        {
            final List<String> u = Py.Re.INSTANCE.findAll("^([^\\n]*" + name +
                    "[^\\n]*\\n?(?:[ \\t].*?(?:\\n|$))*)", source, Py.Re.IGNORECASE | Py.Re.MULTILINE);

            for (int i = 0; i < u.size(); i++) {
                u.set(i, u.get(i).trim());
            }

            return u;
        }
    }

    static List<Option> parseDefaults(final String doc) {
        final List<Option> defaults = Py.INSTANCE.list();

        for (String s : parseSection("options:", doc)) {
            s = Py.INSTANCE.partition(s, ":")[2];

            List<String> split;

            split = Py.Re.INSTANCE.split("\\n *(-\\S+?)", "\n" + s);
            split.remove(0);

            {
                final List<String> u = Py.INSTANCE.list();

                for (int i = 1; i < split.size(); i += 2) {
                    u.add(split.get(i - 1) + split.get(i));
                }

                split = u;
            }

            {
                for (final String $s : split) {
                    if ($s.startsWith("-")) {
                        defaults.add(Option.parse($s));
                    }
                }
            }
        }

        return defaults;
    }

    static String formalUsage(String section) {
        {
            final String[] u = Py.INSTANCE.partition(section, ":");
            section = u[2];
        }

        final List<String> pu = Py.INSTANCE.split(section);

        {
            final StringBuilder sb = new StringBuilder();

            sb.append("( ");

            final String u = pu.remove(0);

            if (!pu.isEmpty()) {
                for (final String s : pu) {
                    if (s.equals(u)) {
                        sb.append(") | (");
                    } else {
                        sb.append(s);
                    }

                    sb.append(" ");
                }

                sb.setLength(sb.length() - 1);
            }

            sb.append(" )");

            return sb.toString();
        }
    }

    static void extras(final boolean help, final String version,
                       final List<? extends LeafPattern> options, final String doc) {
        boolean u;

        {
            u = false;

            if (help) {
                for (final LeafPattern o : options) {
                    if ("-h".equals(o.getName()) | "--help".equals(o.getName())) {
                        if (Py.INSTANCE.bool(o.getValue())) {
                            u = true;
                            break;
                        }
                    }
                }
            }
        }

        if (u) {
            throw new DocoptExitException(0, doc.replaceAll("^\\n+|\\n+$", ""),
                    false);
        }

        {
            u = false;

            if (version != null && !version.equals("")) {
                for (final LeafPattern o : options) {
                    if ("--version".equals(o.getName())) {
                        u = true;
                        break;
                    }
                }
            }
        }

        if (u) throw new DocoptExitException(0, version, false);
    }
}
