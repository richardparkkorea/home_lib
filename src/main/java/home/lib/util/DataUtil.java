package home.lib.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

@Deprecated
public class DataUtil {

	/**
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	
	

	
	/**
	 * 
	 * @param <T>
	 * @param <E>
	 * @param map
	 * @param value
	 * @return
	 */
	public static <T, E> Set<T> getKeys(Map<T, E> map, E value) {
	    Set<T> keys = new HashSet<T>();
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            keys.add(entry.getKey());
	        }
	    }
	    return keys;
	}
	
	
}
