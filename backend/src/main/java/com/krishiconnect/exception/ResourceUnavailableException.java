package com.krishiconnect.exception;

import java.util.List;

/** Thrown when equipment or a specialist is already booked for part of the requested time. */
public class ResourceUnavailableException extends ConflictException {

    private final List<String> resources;

    public ResourceUnavailableException(List<String> resources) {
        super(resources.size() == 1
                ? resources.get(0) + " is already booked for these dates"
                : String.join(" and ", resources) + " are already booked for these dates");
        this.resources = List.copyOf(resources);
    }

    public List<String> getResources() {
        return resources;
    }
}
