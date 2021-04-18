package org.docopt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MyTest {
    public MyTest(String doc, List<String> argv, Object expected) {
        this.doc = doc;
        this.argv = argv;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> c = new ArrayList<>();
        final URL url = MyTest.class.getResource("/testcases.docopt");

        for (TestDefinition d : Helper.INSTANCE.getDefs(url)) {
            c.add(new Object[]{d.getDoc(), d.getArgv(), d.getExpected()});
        }
        return c;
    }

    private final String doc;
    private final List<String> argv;
    private final Object expected;

    @Test
    public void test() {
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
