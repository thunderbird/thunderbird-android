package com.fsck.k9.mail.store.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;

public class GetItemEstimateParser extends Parser {
	
	private String mCollectionId;
	private int mEstimate;
	
	public GetItemEstimateParser(InputStream in) throws IOException {
		super(in);
	}

	@Override
	public boolean parse() throws IOException {
        boolean res = true;
        if (nextTag(START_DOCUMENT) != Tags.GIE_GET_ITEM_ESTIMATE) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            switch (tag) {
                case Tags.GIE_RESPONSE:
                    res = parseGIEResponse() && res;
                    break;
                default:
                    skipTag();
            }
        }
        return res;
	}
	
	private boolean parseGIEResponse() throws IOException {
		if (nextTag(Tags.GIE_RESPONSE) == Tags.GIE_STATUS) {
			int status = getValueInt();
			if (status != 1)
				return false;
			
			if (nextTag(Tags.GIE_RESPONSE) == Tags.GIE_COLLECTION) {
				String collectionId;
				int estimate;
				
				if (nextTag(Tags.GIE_COLLECTION) == Tags.GIE_COLLECTION_ID) {
					collectionId = getValue();
				} else return false;
				
				if (nextTag(Tags.GIE_COLLECTION) == Tags.GIE_ESTIMATE) {
					estimate = getValueInt();
				} else return false;
				
				mCollectionId = collectionId;
				mEstimate = estimate;
				return true;
			}
		}
		return false;
	}
	
	public String getCollectionId() {
		return mCollectionId;
	}
	
	public int getEstimate() {
		return mEstimate;
	}

}
