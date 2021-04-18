package org.docopt;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Docopt {

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

        final List<String> usageSections = DocoptStatic.parseSection("usage:", doc);

        if (usageSections.size() == 0) {
            throw new DocoptLanguageError(
                    "\"usage:\" (case-insensitive) not found.");
        }

        if (usageSections.size() > 1) {
            throw new DocoptLanguageError(
                    "More than one \"usage:\" (case-insensitive).");
        }

        usage = usageSections.get(0);
        options = DocoptStatic.parseDefaults(doc);
        pattern = DocoptStatic.parsePattern(DocoptStatic.formalUsage(usage), options);
    }


    public Docopt(final InputStream stream) {
        this(DocoptStatic.read(stream));
    }


    public Docopt withExit(final boolean enabled) {
        this.exit = enabled;
        return this;
    }

    private Map<String, Object> doParse(final List<String> argv) {
        final List<LeafPattern> $argv = DocoptStatic.parseArgv(
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

        DocoptStatic.extras(help, version, $argv, doc);

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
