package net.elodina.xmlavro;

public class ConverterException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5570879940094819474L;

	public ConverterException(String message) {
		super(message);
	}

	public ConverterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConverterException(Throwable cause) {
		super(cause);
	}
}
