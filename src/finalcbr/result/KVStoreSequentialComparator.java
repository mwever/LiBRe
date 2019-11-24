package finalcbr.result;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.kvstore.IKVStore;

public class KVStoreSequentialComparator implements Comparator<IKVStore> {

	private static final Logger LOGGER = LoggerFactory.getLogger(KVStoreSequentialComparator.class);

	private final String[] sortKeys;

	/**
	 * Default c'tor initializing the comparator for a set of keys for which the kvstores are to be sorted.
	 *
	 * @param sortKeys The array of keys for which to sort the KVStoreCollection.
	 */
	public KVStoreSequentialComparator(final String... sortKeys) {
		this.sortKeys = sortKeys;
	}

	@Override
	public int compare(final IKVStore arg0, final IKVStore arg1) {
		for (String sortKey : this.sortKeys) {
			Integer compare = null;

			try {
				compare = arg0.getAsInt(sortKey).compareTo(arg1.getAsInt(sortKey));
			} catch (Exception e) {
				try {
					compare = arg0.getAsLong(sortKey).compareTo(arg1.getAsLong(sortKey));
				} catch (Exception e1) {
					try {
						compare = arg0.getAsString(sortKey).compareTo(arg1.getAsString(sortKey));
					} catch (Exception e2) {
						LOGGER.warn("The values of the key {} are neither int nor long nor string. This type of value is thus not supported for sorting and the key is skipped.", sortKey);
					}
				}
			}

			if (compare == null || compare == 0) {
				continue;
			}
			return compare;
		}
		return 0;
	}

}
