package org.flatizy.flatizy.entity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApartmentSaveResponse {
    private boolean success;
    private String message;
    private int saved;
    private int skipped;

    public ApartmentSaveResponse(boolean success, String message, int saved, int skipped) {
        this.success = success;
        this.message = message;
        this.saved = saved;
        this.skipped = skipped;
    }
}
