package textmining.wanderlust.nlp.domain;

public enum EntityType {

	ANNOTATED,

	NOUN_PHRASE,

	ALL;

	public static EntityType getTypeByName(String name) {
		return EntityType.valueOf(EntityType.class, name.toUpperCase());
	}

}
