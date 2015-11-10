package org.saiku.web.rest.objects.repository;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "FILE", value = RepositoryFileObject.class),
        @JsonSubTypes.Type(name = "FOLDER", value = RepositoryFolderObject.class)
})
public interface IRepositoryObject {

    public enum Type {
        FOLDER, FILE
    }

    public String getId();

    public Type getType();

    public String getName();

    public String getPath();

}
