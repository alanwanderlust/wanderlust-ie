package textmining.wanderlust.nlp.domain;

import java.io.Serializable;

// TODO: Auto-generated Javadoc

/**
 * The Class DepLink.
 */
public class DepLink implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8171736316052514432L;

	/** The origin word. */
	private DepWord originWord;

	/** The link label. */
	private String linkLabel;

	/** The target word. */
	private DepWord targetWord;

	/**
	 * Instantiates a new dep link.
	 * 
	 * @param originWord
	 *            the origin word
	 * @param linkLabel
	 *            the link label
	 * @param targetWord
	 *            the target word
	 */
	public DepLink(DepWord originWord, String linkLabel, DepWord targetWord) {
		super();
		this.originWord = originWord;
		this.linkLabel = linkLabel;
		this.targetWord = targetWord;
	}

	@Override
	public String toString() {
		return linkLabel + " from " + originWord.getWordName() + " to "
				+ targetWord.getWordName();
	}

	/**
	 * Gets the origin word.
	 * 
	 * @return the origin word
	 */
	public DepWord getOriginWord() {
		return originWord;
	}

	/**
	 * Sets the origin word.
	 * 
	 * @param originWord
	 *            the new origin word
	 */
	public void setOriginWord(DepWord originWord) {
		this.originWord = originWord;
	}

	/**
	 * Gets the link label.
	 * 
	 * @return the link label
	 */
	public String getLinkLabel() {
		return linkLabel;
	}

	/**
	 * Sets the link label.
	 * 
	 * @param linkLabel
	 *            the new link label
	 */
	public void setLinkLabel(String linkLabel) {
		this.linkLabel = linkLabel;
	}

	/**
	 * Gets the target word.
	 * 
	 * @return the target word
	 */
	public DepWord getTargetWord() {
		return targetWord;
	}

	/**
	 * Sets the target word.
	 * 
	 * @param targetWord
	 *            the new target word
	 */
	public void setTargetWord(DepWord targetWord) {
		this.targetWord = targetWord;
	}

}
