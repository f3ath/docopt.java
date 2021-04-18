package org.docopt;

import java.util.List;

/**
 * Leaf/terminal node of a pattern tree.
 */
abstract class LeafPattern extends Pattern {

	static class SingleMatchResult {

		private final Integer position;

		private final LeafPattern match;

		public SingleMatchResult(final Integer position, final LeafPattern match) {
			this.position = position;
			this.match = match;
		}

		public Integer getPosition() {
			return position;
		}

		public LeafPattern getMatch() {
			return match;
		}

		@Override
		public String toString() {
			return String.format("%s(%d, %s)", getClass().getSimpleName(),
					position, match);
		}
	}

	private final String name;

	private Object value;

	public LeafPattern(final String name, final Object value) {
		this.name = name;
		this.value = value;
	}

	public LeafPattern(final String name) {
		this(name, null);
	}

	@Override
	public String toString() {
		//        if (o == null) {
//            return "null";
//        }
//
//        if (o instanceof String) {
//            return "\"" + o + "\"";
//        }
//
//        if (o instanceof Object[]) {
//            return Arrays.toString((Object[]) o);
//        }
//
//        return o.toString();
		//        if (o == null) {
//            return "null";
//        }
//
//        if (o instanceof String) {
//            return "\"" + o + "\"";
//        }
//
//        if (o instanceof Object[]) {
//            return Arrays.toString((Object[]) o);
//        }
//
//        return o.toString();
		return String.format("%s(%s, %s)", getClass().getSimpleName(),
				Py.INSTANCE.repr(name), Py.INSTANCE.repr(value));
	}

	@Override
	protected final List<Pattern> flat(final Class<?>... types) {
		// >>> [self] if not types or type(self) in types else []
		{

			if (!Py.INSTANCE.bool(types) || Py.INSTANCE.in(getClass(), types)) {
				return Py.INSTANCE.list((Pattern) this);
			}

			return Py.INSTANCE.list();
		}
	}

	@Override
	protected MatchResult match(final List<LeafPattern> left,
			List<LeafPattern> collected) {
		// >>> collected = [] if collected is None else collected
		if (collected == null) {
			collected = Py.INSTANCE.list();
		}

		Integer pos;
		LeafPattern match;

		// >>> pos, match = self.single_match(left)
		{
			final SingleMatchResult m = singleMatch(left);
			pos = m.getPosition();
			match = m.getMatch();
		}

		if (match == null) {
			return new MatchResult(false, left, collected);
		}

		List<LeafPattern> left_;

		// >>> left_ = left[:pos] + left[pos + 1:]
		{
			left_ = Py.INSTANCE.list();
			left_.addAll(left.subList(0, pos));

			if ((pos + 1) < left.size()) {
				left_.addAll(left.subList(pos + 1, left.size()));
			}
		}

		List<LeafPattern> sameName;

		// >>> same_name = [a for a in collected if a.name == self.name]
		{
			sameName = Py.INSTANCE.list();

			for (final LeafPattern a : collected) {
				if (name.equals(a.getName())) {
					sameName.add(a);
				}
			}
		}

		Object increment;

		if ((value instanceof Integer) || (value instanceof List)) {
			if (value instanceof Integer) {
				increment = 1;
			}
			else {
				final Object v = match.getValue();
				increment = (v instanceof String) ? Py.INSTANCE.list(v) : v;
			}

			if (sameName.isEmpty()) {
				match.setValue(increment);
				//        final List<T> c = new ArrayList<T>(a.size() + b.size());
//
//        c.addAll(a);
//        c.addAll(b);
//
//        return c;
				return new MatchResult(true, left_,
						Py.INSTANCE.plus(collected, Py.INSTANCE.list(match)));
			}

			// >>> same_name[0].value += increment
			{
				final LeafPattern p = sameName.get(0);
				final Object v = p.getValue();

				if (v instanceof Integer) {
					final Integer a = (Integer) v;
					final Integer b = (Integer) increment;
					p.setValue(a + b);
				}
				else if (v instanceof List) {
					@SuppressWarnings("unchecked")
					final List<LeafPattern> a = (List<LeafPattern>) v;
					@SuppressWarnings("unchecked")
					final List<LeafPattern> b = (List<LeafPattern>) increment;
					a.addAll(b);
				}
			}

			// TODO: Should collected be copied to a new list?
			return new MatchResult(true, left_, collected);
		}

		//        final List<T> c = new ArrayList<T>(a.size() + b.size());
//
//        c.addAll(a);
//        c.addAll(b);
//
//        return c;
		return new MatchResult(true, left_, Py.INSTANCE.plus(collected, Py.INSTANCE.list(match)));
	}

	protected abstract SingleMatchResult singleMatch(List<LeafPattern> left);

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LeafPattern other = (LeafPattern) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		}
		else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
}