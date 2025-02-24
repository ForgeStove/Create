package com.simibubi.create.foundation.data;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
/**
 * @deprecated Use {@link AbstractRegistrate#addRawLang} or {@link AbstractRegistrate#addDataGenerator} with
 * {@link ProviderType#LANG} instead.
 */
public class LangEntry {
	static final String ENTRY_FORMAT = "\t\"%s\": %s,\n";
	private final String key;
	private final String value;
	LangEntry(String key, String value) {
		this.key = key;
		this.value = value;
	}
	@Override public String toString() {
		return String.format(ENTRY_FORMAT, key, LangMerger.GSON.toJson(value, String.class));
	}
}
