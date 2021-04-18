package org.docopt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class DocoptTest extends TestCase {

    public static Test suite() {
        try {
            return getTestSuite();
        } catch (final IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    @NotNull
    private static TestSuite getTestSuite() throws IOException {
        final URL url;

        url = DocoptTest.class.getResource("/testcases.docopt");

        System.out.println("Generating test cases from " + url);

        final String name = Helper.INSTANCE.pureBaseName(url);

        String raw = Docopt.read(url.openStream());

        raw = Pattern.compile("#.*$", Pattern.MULTILINE).matcher(raw)
                .replaceAll("");

        if (raw.startsWith("\"\"\"")) {
            raw = raw.substring(3);
        }

        int index = 0;

        final TestSuite suite = new TestSuite("docopt");
        final List<DocoptTest> tests = new ArrayList<>();

        for (final String fixture : raw.split("r\"\"\"")) {
            if (fixture.isEmpty()) {
                continue;
            }

            final String doc1;
            final String body;

            {
                final String[] u = Py.INSTANCE.partition(fixture, "\"\"\"");
                doc1 = u[0];
                body = u[2];
            }

            boolean first = true;

            for (final String _case : body.split("\\$")) {
                if (first) {
                    first = false;
                    continue;
                }

                final String argv1;
                final String expect;

                {
                    final String[] u = Py.INSTANCE.partition(_case.trim(), "\n");
                    argv1 = u[0];
                    expect = u[2];
                }

                DocoptTest test = new DocoptTest(String.format("%s_%d", name,
                        ++index), doc1, Helper.INSTANCE.argv(argv1), Helper.INSTANCE.expect(expect));

                suite.addTest(test);
            }
        }

        return suite;
    }


    private final String doc;
    private final List<String> argv;
    private final Object expected;

    DocoptTest(final String name, final String doc,
               final List<String> argv, final Object expected) {
        super(name);

        this.doc = doc;
        this.argv = argv;
        this.expected = expected; // TODO: Make a defensive copy?
    }

    @Override
    protected void runTest() {
        Object actual;
        try {
            actual = new Docopt(doc)
                    .withStdOut(null)
                    .withStdErr(null)
                    .withExit(false)
                    .parse(argv);
        } catch (final DocoptExitException e) {
            actual = "\"user-error\"";
        }

        final String message = String.format(
                "\n\"\"\"%s\"\"\"\n$ %s\n\b", doc, Helper.INSTANCE.argv(argv));

        assertEquals(message, expected, actual);
    }
}
