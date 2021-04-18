package org.docopt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.docopt.Python.partition;

public final class DocoptTest extends TestCase {

    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE =
            new TypeReference<Map<String, Object>>() {
            };

    public static Test suite() {
        try {
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

            for (final String fixture : raw.split("r\"\"\"")) {
                if (fixture.isEmpty()) {
                    continue;
                }

                final String doc1;
                final String body;

                {
                    final String[] u = partition(fixture, "\"\"\"");
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
                        final String[] u = partition(_case.trim(), "\n");
                        argv1 = u[0];
                        expect = u[2];
                    }

                    suite.addTest(new DocoptTest(String.format("%s_%d", name,
                            ++index), doc1, Helper.INSTANCE.argv(argv1), expect(expect)));
                }
            }

            return suite;
        } catch (final IOException e) {
            final String message;

            if (e instanceof FileNotFoundException) {
                message = "No such file";
            } else {
                message = e.getMessage();
            }

            throw new AssertionError(message);
        }
    }

    private static Object expect(final String expect) {
        if ("\"user-error\"".equals(expect)) {
            return "\"user-error\"";
        }

        try {
            return new ObjectMapper().readValue(expect, TYPE_REFERENCE);
        } catch (final IOException e) {
            throw new IllegalStateException(
                    "could not parse JSON object from:\n" + expect, e);
        }
    }

    private final String doc;
    private final List<String> argv;
    private final Object expected;

    private DocoptTest(final String name, final String doc,
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
            actual = new Docopt(doc).withStdOut(null).withStdErr(null)
                    .withExit(false).parse(argv);
        } catch (final DocoptExitException e) {
            actual = "\"user-error\"";
        }

        final String message = String.format(
                "\n\"\"\"%s\"\"\"\n$ %s\n\b", doc, Helper.INSTANCE.argv(argv));

        assertEquals(message, expected, actual);
    }
}
