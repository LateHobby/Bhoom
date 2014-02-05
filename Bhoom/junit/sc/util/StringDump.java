package sc.util;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Utility class to dump {@code Object}s to string using reflection and recursion.
 */
public class StringDump {

    /**
     * Uses reflection and recursion to dump the contents of the given object using a custom, JSON-like notation (but not JSON). Does not format static fields.<p>
     * @see #dump(Object, boolean, IdentityHashMap, int)
     * @param object the {@code Object} to dump using reflection and recursion
     * @return a custom-formatted string representing the internal values of the parsed object
     */
    public static String dump(Object object) {
        return dump(object, false, new IdentityHashMap<Object, Object>(), 0);
    }

    /**
     * Uses reflection and recursion to dump the contents of the given object using a custom, JSON-like notation (but not JSON).<p>
     * Parses all fields of the runtime class including super class fields, which are successively prefixed with "{@code super.}" at each level.<p>
     * {@code Number}s, {@code enum}s, and {@code null} references are formatted using the standard {@link String#valueOf()} method.
     * {@code CharSequences}s are wrapped with quotes.<p>
     * The recursive call invokes only one method on each recursive call, so limit of the object-graph depth is one-to-one with the stack overflow limit.<p>
     * Backwards references are tracked using a "visitor map" which is an instance of {@link IdentityHashMap}.
     * When an existing object reference is encountered the {@code "sysId"} is printed and the recursion ends.<p>
     * 
     * @param object             the {@code Object} to dump using reflection and recursion
     * @param isIncludingStatics {@code true} if {@code static} fields should be dumped, {@code false} to skip them
     * @return a custom-formatted string representing the internal values of the parsed object
     */
    public static String dump(Object object, boolean isIncludingStatics) {
        return dump(object, isIncludingStatics, new IdentityHashMap<Object, Object>(), 0);
    }

    private static String dump(Object object, boolean isIncludingStatics, IdentityHashMap<Object, Object> visitorMap, int tabCount) {
        if (object == null ||
                object instanceof Number || object instanceof Character || object instanceof Boolean ||
                object.getClass().isPrimitive() || object.getClass().isEnum()) {
            return String.valueOf(object);
        }

        StringBuilder builder = new StringBuilder();
        int           sysId   = System.identityHashCode(object);
        if (object instanceof CharSequence) {
            builder.append("\"").append(object).append("\"");
        }
        else if (visitorMap.containsKey(object)) {
            builder.append("(sysId#").append(sysId).append(")");
        }
        else {
            visitorMap.put(object, object);

            StringBuilder tabs = new StringBuilder();
            for (int t = 0; t < tabCount; t++) {
                tabs.append("\t");
            }
            if (object.getClass().isArray()) {
                builder.append("[").append(object.getClass().getName()).append(":sysId#").append(sysId);
                int length = Array.getLength(object);
                for (int i = 0; i < length; i++) {
                    Object arrayObject = Array.get(object, i);
                    String dump        = dump(arrayObject, isIncludingStatics, visitorMap, tabCount + 1);
                    builder.append("\n\t").append(tabs).append("\"").append(i).append("\":").append(dump);
                }
                builder.append(length == 0 ? "" : "\n").append(length == 0 ? "" : tabs).append("]");
            }
            else {
                // enumerate the desired fields of the object before accessing
                TreeMap<String, Field> fieldMap    = new TreeMap<String, Field>();  // can modify this to change or omit the sort order
                StringBuilder          superPrefix = new StringBuilder();
                for (Class<?> clazz = object.getClass(); clazz != null && !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        Field field = fields[i];
                        if (isIncludingStatics || !Modifier.isStatic(field.getModifiers())) {
                            fieldMap.put(superPrefix + field.getName(), field);
                        }
                    }
                    superPrefix.append("super.");
                }

                builder.append("{").append(object.getClass().getName()).append(":sysId#").append(sysId);
                for (Entry<String, Field> entry : fieldMap.entrySet()) {
                    String name  = entry.getKey();
                    Field  field = entry.getValue();
                    String dump;
                    try {
                        boolean wasAccessible = field.isAccessible();
                        field.setAccessible(true);
                        Object  fieldObject   = field.get(object);
                        field.setAccessible(wasAccessible);  // the accessibility flag should be restored to its prior ClassLoader state
                        dump                  = dump(fieldObject, isIncludingStatics, visitorMap, tabCount + 1);
                    }
                    catch (Throwable e) {
                        dump = "!" + e.getClass().getName() + ":" + e.getMessage();
                    }
                    builder.append("\n\t").append(tabs).append("\"").append(name).append("\":").append(dump);
                }
                builder.append(fieldMap.isEmpty() ? "" : "\n").append(fieldMap.isEmpty() ? "" : tabs).append("}");
            }
        }
        return builder.toString();
    }
}