package org.docopt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Python {

    static final class Re {

        public static final int IGNORECASE = Pattern.CASE_INSENSITIVE;

        public static final int MULTILINE = Pattern.MULTILINE
                | Pattern.UNIX_LINES;

        public static List<String> findAll(final String pattern,
                                           final String string, final int flags) {
            return findAll(Pattern.compile(pattern, flags), string);
        }

        public static List<String> findAll(final Pattern pattern,
                                           final String string) {
            final Matcher matcher = pattern.matcher(string);

            final List<String> result = list();

            while (matcher.find()) {
                if (matcher.groupCount() == 0) {
                    result.add(matcher.group());
                } else {
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        final String match = matcher.group(i + 1);

                        if (match != null) {
                            result.add(match);
                        }
                    }
                }
            }

            return result;
        }

        /**
         * Determines if {@code pattern} contains at least one capturing group.
         */
        private static boolean hasGrouping(final String pattern) {
            int i = -1;

            // Find the potential beginning of a group by looking for a left
            // parenthesis character.
            while ((i = pattern.indexOf('(', i + 1)) != -1) {
                int c = 0;

                // Count the number of escape characters immediately preceding
                // the
                // left parenthesis character.
                for (int j = i - 1; j > -1; j--) {
                    if (pattern.charAt(j) != '\\') {
                        break;
                    }

                    c++;
                }

                // If there is an even number of consecutive escape characters,
                // the character is not escaped and begins a group.
                if (c % 2 == 0) {
                    return true;
                }
            }

            return false;
        }

        public static List<String> split(final String pattern,
                                         final String string) {
            if (!hasGrouping(pattern)) {
                return list(string.split(pattern));
            }

            final Matcher matcher = Pattern.compile(pattern, 0).matcher(string);

            final List<String> matches = list();

            int start = 0;

            while (matcher.find()) {
                matches.add(string.substring(start, matcher.start()));

                for (int i = 0; i < matcher.groupCount(); i++) {
                    matches.add(matcher.group(i + 1));
                }

                start = matcher.end();
            }

            matches.add(string.substring(start));

            return matches;
        }

        public static String sub(final String pattern, final String repl,
                                 final String string) {
            return Pattern.compile(pattern, 0).matcher(string).replaceAll(repl);
        }

        private Re() {
            // Prevent instantiation.
        }
    }

    public static <T> List<T> list(final Iterable<? extends T> elements) {
        return Py.INSTANCE.list(elements);
    }

    public static <T> List<T> list(final T[] elements) {
        return Py.INSTANCE.list(elements);
    }

    public static <T> List<T> list(final T element) {
        return Py.INSTANCE.list(element);
    }

    public static <T> List<T> list() {
        return Py.INSTANCE.list();
    }

    public static <T> int count(final List<T> self, final T obj) {
        int count = 0;

        for (final T element : self) {
            if (element.equals(obj)) {
                count++;
            }
        }

        return count;
    }

    public static <T> Set<T> set(final Iterable<T> elements) {
        final Set<T> set = new HashSet<T>();

        for (final T element : elements) {
            set.add(element);
        }

        return set;
    }

    public static String join(final String self, final Iterable<?> iterable) {
        final Iterator<?> i = iterable.iterator();

        if (!i.hasNext()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();

        while (i.hasNext()) {
            sb.append(i.next());
            sb.append(self);
        }

        sb.setLength(sb.length() - self.length());

        return sb.toString();
    }

    public static String[] partition(final String self, final String sep) {
        final int i = self.indexOf(sep);

        if (i == -1) {
            return new String[]{self, "", ""};
        }

        // Always <= s.length
        final int j = i + sep.length();

        return new String[]{self.substring(0, i), sep,
                (j < self.length()) ? self.substring(j) : ""};
    }

    public static boolean isUpper(final String self) {
        boolean result = false;

        for (final char c : self.toCharArray()) {
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    result = true;
                } else {
                    return false;
                }
            }
        }

        return result;
    }

    public static List<String> split(final String self) {
        return list(self.trim().split("\\s+"));
    }

    private Python() {
        // Prevent instantiation.
    }
}
