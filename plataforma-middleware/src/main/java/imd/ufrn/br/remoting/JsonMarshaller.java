package imd.ufrn.br.remoting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import imd.ufrn.br.exceptions.MarshallingException;

import java.io.IOException;
import java.util.List;

public class JsonMarshaller {

    private final ObjectMapper objectMapper;

    public JsonMarshaller() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String serialize(Object data) throws MarshallingException {
        if (data == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new MarshallingException("Error marshalling object to JSON: " + e.getMessage(), e);
        }
    }

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