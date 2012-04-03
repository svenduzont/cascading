package cascading.scheme;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cascading.tuple.Fields;
import cascading.tuple.Tuple;

public class EscapedDelimitedParser extends DelimitedParser {

	private static final long serialVersionUID = -3973354269374253629L;
	private char escapeChar;
	private char quoteChar;
	private char delimiterChar;
	private boolean appendEscapeChar;

	public EscapedDelimitedParser(String delimiter, String quote, String escape, Class[] types, boolean strict,
			boolean safe, Fields sourceFields, Fields sinkFields) {
		super(delimiter, quote, types, strict, safe, sourceFields, sinkFields);
		this.escapeChar = escape.charAt(0);
		this.quoteChar = quote.charAt(0);
		this.delimiterChar = delimiter.charAt(0);
	}

	@Override
	public Object[] cleanSplit(Object[] split, Pattern cleanPattern, Pattern escapePattern, String quote) {
		return split;
	}

	@Override
	public String[] createSplit(String value, Pattern splitPattern, int numValues) {
		if (value == null) {
			return null;
		}
		List<String> tokensOnThisLine = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		boolean inQuotes = false;
		do {
			if (inQuotes) {
				// continuing a quoted section, reappend newline 
				sb.append("\n");
				break;
			}
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == quoteChar) {
					// this gets complex... the quote may end a quoted block, or 
					// escape another quote. 
					// do a 1-char lookahead: 
					if (inQuotes // we are in quotes, therefore there can be 
							//escaped quotes in here. 
							&& value.length() > (i + 1) // there is indeed another 
							//character to check. 
							&& value.charAt(i + 1) == quoteChar) { // ..and that 
						//char. is a quote also. 
						// we have two quote chars in a row == one quote char, so 
						//consume them both and 
						// put one on the token. we do *not* exit the quoted text. 
						sb.append(value.charAt(i + 1));
						i++;
					} else {
						inQuotes = !inQuotes;
						// the tricky case of an embedded quote in the middle: 
						//a,bc"d"ef,g 
						if (i > 2 // not on the begining of the line 
								&& value.charAt(i - 1) != delimiterChar // not at 
								//the begining of an escape sequence 
								&& value.length() > (i + 1) && value.charAt(i + 1) != delimiterChar // not at the 
						// 
						//end 
						// of an escape 
						// sequence 
						) {
							sb.append(c);
						}
					}
				} else if (c == delimiterChar && !inQuotes) {
					if (sb.length() == 0) {
						tokensOnThisLine.add(null);
					} else {
						tokensOnThisLine.add(sb.toString());
					}
					sb = new StringBuffer(); // start work on next token 
				} else {
					sb.append(c);
					if (c == escapeChar) {
						sb.append(value.charAt(i + 1));
						i++;
					}
				}
			}
		} while (inQuotes);
		tokensOnThisLine.add(sb.toString());
		return (String[]) tokensOnThisLine.toArray(new String[0]);
	}

	public String joinLine(Tuple tuple, StringBuilder buffer) {
		try {
			if (quote != null)
				return joinWithQuote(tuple, buffer);

			return joinNoQuote(tuple, buffer);
		} finally {
			buffer.setLength(0);
		}
	}

	private String joinWithQuote(Tuple tuple, StringBuilder buffer) {
		int count = 0;

		for (Object value : tuple) {
			if (count != 0)
				buffer.append(delimiter);

			if (value != null) {
				String valueString = value.toString();

				if (valueString.contains(quote))
					valueString = valueString.replaceAll(quote, escapeChar + quote);

				if (valueString.contains(delimiter))
					valueString = quote + valueString.replaceAll(delimiter, escapeChar + delimiter) + quote;

				buffer.append(valueString);
			}

			count++;
		}

		return buffer.toString();
	}

	private String joinNoQuote(Tuple tuple, StringBuilder buffer) {
		int count = 0;

		for (Object value : tuple) {
			if (count != 0)
				buffer.append(delimiter);

			if (value != null)
				buffer.append(value);

			count++;
		}

		return buffer.toString();
	}
}
