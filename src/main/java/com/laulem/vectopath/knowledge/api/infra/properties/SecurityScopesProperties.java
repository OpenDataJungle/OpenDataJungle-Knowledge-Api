package com.laulem.vectopath.knowledge.api.infra.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.scopes")
public class SecurityScopesProperties {
    private Search search = new Search();
    private Resources resources = new Resources();

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public static class Search {
        private String semantic;

        public String getSemantic() {
            return semantic;
        }

        public void setSemantic(String semantic) {
            this.semantic = semantic;
        }
    }

    public static class Resources {
        private String read;
        private String write;
        private String delete;

        public String getRead() {
            return read;
        }

        public void setRead(String read) {
            this.read = read;
        }

        public String getWrite() {
            return write;
        }

        public void setWrite(String write) {
            this.write = write;
        }

        public String getDelete() {
            return delete;
        }

        public void setDelete(String delete) {
            this.delete = delete;
        }
    }
}
