package com.zego.mediaplayer.entity;

import java.io.Serializable;

public class ZGResourcesInfo implements Serializable {
        private String mediaNameKey;
        private String mediaFileTypeKey;
        private String mediaSourceTypeKey;
        private String mediaUrlKey;

        public String getMediaNameKey() {
            return mediaNameKey;
        }

        public ZGResourcesInfo mediaNameKey(String mediaNameKey) {
            this.mediaNameKey = mediaNameKey;
            return this;
        }

        public String getMediaFileTypeKey() {
            return mediaFileTypeKey;
        }

        public ZGResourcesInfo mediaFileTypeKey(String mediaFileTypeKey) {
            this.mediaFileTypeKey = mediaFileTypeKey;
            return this;
        }

        public String getMediaSourceTypeKey() {
            return mediaSourceTypeKey;
        }

        public ZGResourcesInfo mediaSourceTypeKey(String mediaSourceTypeKey) {
            this.mediaSourceTypeKey = mediaSourceTypeKey;
            return this;
        }

        public String getMediaUrlKey() {
            return mediaUrlKey;
        }

        public ZGResourcesInfo mediaUrlKey(String mediaUrlKey) {
            this.mediaUrlKey = mediaUrlKey;
            return this;
        }
    }