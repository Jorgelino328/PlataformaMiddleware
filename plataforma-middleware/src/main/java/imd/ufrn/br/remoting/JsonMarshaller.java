package imd.ufrn.br.remoting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For Java 8+ Date/Time
import imd.ufrn.br.exceptions.MarshallingException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A Marshaller implementation that uses JSON as the wire format,
 * leveraging the Jackson library for serialization and deserialization.
 */
public class JsonMarshaller {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new JsonMarshaller.
     * Initializes and configures the Jackson ObjectMapper.
     */
    public JsonMarshaller() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

         }

    /**
     * Marshals (serializes) a Java object into a JSON string.
     *
     * @param data The object to marshal. Can be any Java object that Jackson can serialize.
     * @return The JSON string representation of the object.
     * @throws MarshallingException if an error occurs during JSON serialization.
     */
    public String marshal(Object data) throws MarshallingException {
        if (data == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new MarshallingException("Error marshalling object to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Unmarshals (deserializes) a JSON string into a Java object of the specified target type.
     *
     * @param jsonData   The JSON string to unmarshal.
     * @param targetType The {@link Class} of the object to create.
     * @param <T>        The generic type of the target object.
     * @return The deserialized Java object.
     * @throws MarshallingException if an error occurs during JSON deserialization or if jsonData is invalid.
     */
    public <T> T unmarshal(String jsonData, Class<T> targetType) throws MarshallingException {
        if (jsonData == null || jsonData.trim().isEmpty() || "null".equalsIgnoreCase(jsonData.trim())) {
            return null;
        }
        if (targetType == null) {
            throw new MarshallingException("Target type cannot be null for unmarshalling.", null);
        }
        try {
            return objectMapper.readValue(jsonData, targetType);
        } catch (IOException e) {
            throw new MarshallingException("Error unmarshalling JSON to object of type " +
                    targetType.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Unmarshals a JSON array string into an array of Java objects, attempting to convert
     * each element to its corresponding type in {@code paramTypes}.
     * <p>
     * This method is crucial for preparing arguments for method invocation by the {@link Invoker}.
     *
     * @param jsonParamsArray A JSON string representing an array of parameters (e.g., {@code "[10, \"hello\", {\"key\":\"value\"}]"}).
     * @param paramTypes      An array of {@link Class} objects representing the target types for each parameter.
     *                        The length of this array must match the number of elements in the JSON array.
     * @return An array of {@link Object}s, where each object is unmarshalled to its respective type from {@code paramTypes}.
     * @throws MarshallingException if the JSON is not a valid array, if the number of parameters
     *                              does not match {@code paramTypes.length}, or if any individual
     *                              parameter cannot be unmarshalled to its target type.
     */
    public Object[] unmarshalParameters(String jsonParamsArray, Class<?>[] paramTypes) throws MarshallingException {
        if (paramTypes == null) {
            throw new MarshallingException("Parameter types array cannot be null for unmarshalling parameters.", null);
        }

        if (jsonParamsArray == null || jsonParamsArray.trim().isEmpty() || "[]".equals(jsonParamsArray.trim())) {
            if (paramTypes.length == 0) {
                return new Object[0];
            } else {
                throw new MarshallingException("Received empty parameters JSON, but expected " + paramTypes.length + " parameters.", null);
            }
        }

        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            JavaType listType = typeFactory.constructCollectionType(List.class, Object.class);
            List<Object> rawParamsList = objectMapper.readValue(jsonParamsArray, listType);

            if (rawParamsList.size() != paramTypes.length) {
                throw new MarshallingException("Parameter count mismatch. Expected " + paramTypes.length +
                        " parameters, but received " + rawParamsList.size() +
                        " from JSON: " + jsonParamsArray, null);
            }

            Object[] convertedParams = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Object rawParam = rawParamsList.get(i);
                Class<?> targetType = paramTypes[i];

                if (rawParam == null) {
                    convertedParams[i] = null;
                } else {
                    try {
                        convertedParams[i] = objectMapper.convertValue(rawParam, targetType);
                    } catch (IllegalArgumentException e) {
                        throw new MarshallingException("Error converting parameter at index " + i +
                                " from raw type '" + rawParam.getClass().getName() +
                                "' to target type '" + targetType.getName() +
                                "'. Raw value: " + rawParam + ". JSON: " + jsonParamsArray, e);
                    }
                }
            }
            return convertedParams;

        } catch (IOException e) {
            throw new MarshallingException("Error unmarshalling parameters array from JSON: " + jsonParamsArray +
                    ". " + e.getMessage(), e);
        }
    }
}