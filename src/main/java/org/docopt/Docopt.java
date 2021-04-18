package org.docopt;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public final class Docopt {

    private static List<Option> parseLong(final Tokens tokens,
                                          final List<Option> options) {

        String $long;
        String eq;
        String value;

        {
            final String[] a = Py.INSTANCE.partition(tokens.move(), "=");
            $long = a[0];
            eq = a[1];
            value = a[2];
        }

        assert $long.startsWith("--");

        if ("".equals(eq) && "".equals(value)) {
            value = null;
        }

        List<Option> similar;

        {
            similar = Py.INSTANCE.list();

            for (final Option o : options) {
                if ($long.equals(o.getLong())) {
                    similar.add(o);
                }
            }
        }

        if (tokens.getError() == DocoptExitException.class && similar.isEmpty()) {
            {
                for (final Option o : options) {
                    if (o.getLong() != null && o.getLong().startsWith($long)) {
                        similar.add(o);
                    }
                }
            }
        }

        if (similar.size() > 1) {
            List<String> u;

            {
                u = Py.INSTANCE.list();
                for (final Option o : similar) {
                    u.add(o.getLong());
                }
            }

            throw tokens.error("%s is not a unique prefix: %s?", $long,
                    Py.INSTANCE.join(", ", u));
        }

        Option o;

        if (similar.size() < 1) {
            final int argCount = "=".equals(eq) ? 1 : 0;

            o = new Option(null, $long, argCount);

            options.add(o);

            if (tokens.getError() == DocoptExitException.class) {
                o = new Option(null, $long, argCount, (argCount != 0) ? value
                        : true);
            }
        } else {
            {
                final Option u = similar.get(0);
                o = new Option(u.getShort(), u.getLong(), u.getArgCount(),
                        u.getValue());
            }

            if (o.getArgCount() == 0) {
                if (value != null) {
                    throw tokens.error("%s must not have an argument",
                            o.getLong());
                }
            } else {
                if (value == null) {
                    {
                        final String u = tokens.current();
                        if (u == null || "--".equals(u)) {
                            throw tokens.error("%s requires argument",
                                    o.getLong());
                        }
                    }

                    value = tokens.move();
                }
            }

            if (tokens.getError() == DocoptExitException.class) {
                o.setValue((value != null) ? value : true);
            }
        }

        return Py.INSTANCE.list(o);
    }

    /**
     * <pre>
     * shorts ::= '-' ( chars )* [ [ ' ' ] chars ] ;
     * </pre>
     */
    private static List<Option> parseShorts(final Tokens tokens,
                                            final List<Option> options) {
        final String token = tokens.move();
        assert token.startsWith("-") && !token.startsWith("--");
        String left = token.replaceFirst("^-+", "");

        final List<Option> parsed = Py.INSTANCE.list();

        while (!"".equals(left)) {
            final String $short = "-" + left.charAt(0);

            left = left.substring(1);

            List<Option> similar;

            {
                similar = Py.INSTANCE.list();

                for (final Option o : options) {
                    if ($short.equals(o.getShort())) {
                        similar.add(o);
                    }
                }
            }

            if (similar.size() > 1) {
                throw tokens.error("%s is specified ambiguously %d times",
                        $short, similar.size());
            }

            Option o;

            if (similar.size() < 1) {
                o = new Option($short, null, 0);

                options.add(o);

                if (tokens.getError() == DocoptExitException.class) {
                    o = new Option($short, null, 0, true);
                }
            } else {
                {
                    final Option u = similar.get(0);
                    o = new Option($short, u.getLong(), u.getArgCount(),
                            u.getValue());
                }

                String value = null;

                if (o.getArgCount() != 0) {
                    if ("".equals(left)) {
                        {
                            final String u = tokens.current();
                            if (u == null || "--".equals(u)) {
                                throw tokens.error("%s requires argument",
                                        $short);
                            }
                            value = tokens.move();
                        }
                    } else {
                        value = left;
                        left = "";
                    }
                }

                if (tokens.getError() == DocoptExitException.class) {
                    o.setValue((value != null) ? value : true);
                }
            }

            parsed.add(o);
        }

        return parsed;
    }

    private static Required parsePattern(final String source,
                                         final List<Option> options) {
        String source1 = source;
        source1 = Py.Re.INSTANCE.sub("([\\[\\]\\(\\)\\|]|\\.\\.\\.)", " $1 ", source1);
        List<String> $source;
        {
            $source = Py.INSTANCE.list();


            for (final String s : Py.Re.INSTANCE.split("\\s+|(\\S*<.*?>)", source1)) {
                if (Py.INSTANCE.bool(s)) {
                    $source.add(s);
                }
            }
        }

        final Tokens tokens = new Tokens($source, DocoptLanguageError.class);
        final List<? extends Pattern> result = parseExpr(tokens, options);

        if (tokens.current() != null) {
            throw tokens.error("unexpected ending: %s", Py.INSTANCE.join(" ", tokens));
        }

        return new Required(result);
    }

    private static List<? extends Pattern> parseExpr(final Tokens tokens,
                                                     final List<Option> options) {
        List<Pattern> seq = parseSeq(tokens, options);

        if (!"|".equals(tokens.current())) {
            return seq;
        }

        final List<Pattern> result = (seq.size() > 1) ? Py.INSTANCE.list((Pattern) new Required(
                seq))
                : seq;

        while ("|".equals(tokens.current())) {
            tokens.move();
            seq = parseSeq(tokens, options);
            result.addAll((seq.size() > 1) ? Py.INSTANCE.list(new Required(seq)) : seq);
        }

        return (result.size() > 1) ? Py.INSTANCE.list(new Either(result)) : result;
    }

    private static List<Pattern> parseSeq(final Tokens tokens,
                                          final List<Option> options) {
        final List<Pattern> result = Py.INSTANCE.list();


        while (!Py.INSTANCE.in(tokens.current(), null, "]", ")", "|")) {
            List<? extends Pattern> atom = parseAtom(tokens, options);

            if ("...".equals(tokens.current())) {
                atom = Py.INSTANCE.list(new OneOrMore(atom));
                tokens.move();
            }

            result.addAll(atom);
        }

        return result;
    }

    private static List<? extends Pattern> parseAtom(final Tokens tokens,
                                                     final List<Option> options) {
        final String token = tokens.current();

        List<Pattern> result = Py.INSTANCE.list();

        if ("(".equals(token) || "[".equals(token)) {
            tokens.move();

            String matching;

            {
                final List<? extends Pattern> u = parseExpr(tokens, options);

                if ("(".equals(token)) {
                    matching = ")";
                    result = Py.INSTANCE.list((Pattern) new Required(u));
                } else if ("[".equals(token)) {
                    matching = "]";
                    result = Py.INSTANCE.list((Pattern) new Optional(u));
                } else {
                    throw new IllegalStateException();
                }
            }

            if (!matching.equals(tokens.move())) {
                throw tokens.error("unmatched '%s'", token);
            }

            return Py.INSTANCE.list(result);
        }

        if ("options".equals(token)) {
            tokens.move();
            return Py.INSTANCE.list(new OptionsShortcut());
        }

        if (token.startsWith("--") && !"--".equals(token)) {
            return parseLong(tokens, options);
        }

        if (token.startsWith("-") && !("-".equals(token) || "--".equals(token))) {
            return parseShorts(tokens, options);
        }

        if ((token.startsWith("<") && token.endsWith(">")) || Py.INSTANCE.isUpper(token)) {
            return Py.INSTANCE.list(new Argument(tokens.move()));
        }

        return Py.INSTANCE.list(new Command(tokens.move()));
    }

    private static List<LeafPattern> parseArgv(final Tokens tokens,
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
                parsed.addAll(parseLong(tokens, options));
            } else if (tokens.current().startsWith("-")
                    && !"-".equals(tokens.current())) {
                parsed.addAll(parseShorts(tokens, options));
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

    private static List<Option> parseDefaults(final String doc) {
        final List<Option> defaults = Py.INSTANCE.list();

        for (String s : parseSection("options:", doc)) {
            {
                final String[] u = Py.INSTANCE.partition(s, ":");
                s = u[2];
            }

            List<String> split;

            {
                split = Py.Re.INSTANCE.split("\\n *(-\\S+?)", "\n" + s);
                split.remove(0);
            }

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

    private static List<String> parseSection(final String name,
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

    private static String formalUsage(String section) {
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

    private static void extras(final boolean help, final String version,
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

            if (Py.INSTANCE.bool(version)) {
                for (final LeafPattern o : options) {
                    if ("--version".equals(o.getName())) {
                        u = true;
                        break;
                    }
                }
            }
        }

        if (u) {
            throw new DocoptExitException(0, version, false);
        }
    }

    static String read(final InputStream stream, final String charset) {
        final Scanner scanner = new Scanner(stream, charset);

        try {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            scanner.close();
        }
    }

    private final String doc;

    private final String usage;

    private final List<Option> options;

    private final Required pattern;

    private boolean help = true;

    private String version = null;

    private boolean optionsFirst = false;

    private boolean exit = true;

    private PrintStream out = System.out;

    private PrintStream err = System.err;


    public Docopt(final String doc) {
        this.doc = doc;

        final List<String> usageSections = parseSection("usage:", doc);

        if (usageSections.size() == 0) {
            throw new DocoptLanguageError(
                    "\"usage:\" (case-insensitive) not found.");
        }

        if (usageSections.size() > 1) {
            throw new DocoptLanguageError(
                    "More than one \"usage:\" (case-insensitive).");
        }

        usage = usageSections.get(0);
        options = parseDefaults(doc);
        pattern = parsePattern(formalUsage(usage), options);
    }


    public Docopt(final InputStream stream, final Charset charset) {
        this(read(stream, charset.displayName()));
    }

    public Docopt(final InputStream stream) {
        this(read(stream, "UTF-8"));
    }


    public Docopt withHelp(final boolean enabled) {
        this.help = enabled;
        return this;
    }


    public Docopt withVersion(final String version) {
        this.version = version;
        return this;
    }


    public Docopt withVersion(final InputStream stream, final Charset charset) {
        this.version = read(stream, charset.displayName());
        return this;
    }


    public Docopt withVersion(final InputStream stream) {
        this.version = read(stream, "UTF-8");
        return this;
    }

    public Docopt withOptionsFirst(final boolean enabled) {
        this.optionsFirst = enabled;
        return this;
    }


    public Docopt withExit(final boolean enabled) {
        this.exit = enabled;
        return this;
    }

    private Map<String, Object> doParse(final List<String> argv) {
        final List<LeafPattern> $argv = parseArgv(
                new Tokens(argv, DocoptExitException.class), Py.INSTANCE.list(options), optionsFirst);
        final Set<Pattern> patternOptions = Py.INSTANCE.set(pattern.flat(Option.class));

        for (final Pattern optionsShortcut : pattern
                .flat(OptionsShortcut.class)) {
            {
                final List<Pattern> u = ((BranchPattern) optionsShortcut)
                        .getChildren();
                u.clear();
                u.addAll(Py.INSTANCE.set(options));
                Pattern o = null;
                for (final Iterator<Pattern> i = u.iterator(); i.hasNext(); ) {
                    o = i.next();
                    for (final Pattern x : patternOptions) {
                        if (o.equals(x)) {
                            i.remove();
                            break;
                        }
                    }
                }
            }
        }

        extras(help, version, $argv, doc);

        final MatchResult m = pattern.fix().match($argv);

        if (m.matched() && m.getLeft().isEmpty()) {
            final Map<String, Object> u = new HashMap();

            for (final Pattern p : pattern.flat()) {
                // TODO: Does flat always return LeafPattern objects?
                if (!(p instanceof LeafPattern)) {
                    throw new IllegalStateException();
                }

                final LeafPattern lp = (LeafPattern) p;

                u.put(lp.getName(), lp.getValue());
            }

            for (final LeafPattern p : m.getCollected()) {
                u.put(p.getName(), p.getValue());
            }

            return u;
        }

        throw new DocoptExitException(1, null, true);
    }

    public Map<String, Object> parse(final List<String> argv)
            throws DocoptExitException {
        try {
            return doParse(argv);
        } catch (final DocoptExitException e) {
            if (!exit) {
                throw e;
            }

            final PrintStream ps = (e.getExitCode() == 0) ? out : err;

            if (ps != null) {
                final String message = e.getMessage();

                if (message != null) {
                    ps.println(message);
                }

                if (e.getPrintUsage()) {
                    ps.println(usage);
                }
            }

            System.exit(e.getExitCode());

            throw new IllegalStateException();
        }
    }

    public Map<String, Object> parse(final String... argv) {
        return parse(Arrays.asList(argv));
    }

    Docopt withStdOut(final PrintStream out) {
        this.out = out;
        return this;
    }

    Docopt withStdErr(final PrintStream err) {
        this.err = err;
        return this;
    }
}
