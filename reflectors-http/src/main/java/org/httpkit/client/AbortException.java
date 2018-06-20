package org.httpkit.client;

import org.httpkit.HTTPException;

public class AbortException extends HTTPException {

    private static final long serialVersionUID = 1L;

    public AbortException(String msg) {
        super(msg);
    }

}
