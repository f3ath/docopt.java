package org.docopt;

import java.util.List;

public class Parser {

    static Required parsePattern(final String source,
                                 final List<Option> options) {
        return Helper.INSTANCE.parsePattern(source, options);
    }

    static List<LeafPattern> parseArgv(final Tokens tokens,
                                       final List<Option> options,
                                       final boolean optionsFirst) {

        return Helper.INSTANCE.parseArgv(tokens, options, optionsFirst);
    }


    static List<String> parseSection(final String name,
                                     final String source) {
        return Helper.INSTANCE.parseSection(name, source);
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
        return Helper.INSTANCE.formalUsage(section);
    }

    static void extras(final boolean help, final String version,
                       final List<? extends LeafPattern> options, final String doc) {

        Helper.INSTANCE.extras(help, version, options, doc);

    }
}
