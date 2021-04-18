package org.docopt;


import java.util.ArrayList;
import java.util.List;

final class Tokens extends ArrayList<String> {

	private static final long serialVersionUID = 1L;

	public static Tokens withExitException(final List<String> source) {
		return new Tokens(source, DocoptExitException.class);
	}

	public static Tokens withLanguageError(final List<String> source) {
		return new Tokens(source, DocoptLanguageError.class);
	}

	private final Class<? extends Throwable> error;

	public Tokens(final List<String> source,
			final Class<? extends Throwable> error) {
		// >>> self += source.split() if hasattr(source, 'split') else source
		// In this implementation, source is always a list of strings, so no
		// need to split.
		addAll(source);
		this.error = error;
	}

	public static Tokens fromPattern(String source) {
		source = Py.Re.INSTANCE.sub("([\\[\\]\\(\\)\\|]|\\.\\.\\.)", " $1 ", source);

		List<String> $source;

		// >>> source = [s for s in re.split('\s+|(\S*<.*?>)', source) if s]
		{
			$source = Py.INSTANCE.list();

			//
//            if (!Py.Re.INSTANCE.hasGrouping(pattern)) {
//                return Py.INSTANCE.list(string.split(pattern));
//            }
//
//            final Matcher matcher = Pattern.compile(pattern, 0).matcher(string);
//
//            final List<String> matches = Py.INSTANCE.list();
//
//            int start = 0;
//
//            while (matcher.find()) {
//                matches.add(string.substring(start, matcher.start()));
//
//                for (int i = 0; i < matcher.groupCount(); i++) {
//                    matches.add(matcher.group(i + 1));
//                }
//
//                start = matcher.end();
//            }
//
//            matches.add(string.substring(start));
//
//            return matches;
			for (final String s : Py.Re.INSTANCE.split("\\s+|(\\S*<.*?>)", source)) {
				if (Py.INSTANCE.bool(s)) {
					$source.add(s);
				}
			}
		}

		return Tokens.withLanguageError($source);
	}

	public String move() {
		final String result = isEmpty() ? null : remove(0);
		return result;
	}

	public String current() {
		return isEmpty() ? null : get(0);
	}

	public Class<? extends Throwable> getError() {
		return error;
	}

	public IllegalStateException error(final String format,
			final Object... args) {
		final String message = String.format(format, args);

		if (error == DocoptLanguageError.class) {
			throw new DocoptLanguageError(message);
		}

		if (error == DocoptExitException.class) {
			throw new DocoptExitException(1, message, true);
		}

		return new IllegalStateException("Unexpected exception: "
				+ error.getName());
	}
}