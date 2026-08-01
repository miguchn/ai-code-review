package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 项目表单所需的已授权选项。 */
public class ReviewProjectOptions
{
    private List<Option> businessSystems = new ArrayList<>();
    private List<Option> departments = new ArrayList<>();
    private List<Option> owners = new ArrayList<>();
    private List<Option> credentials = new ArrayList<>();
    private List<Option> models = new ArrayList<>();
    private List<Option> templates = new ArrayList<>();
    private List<String> longLivedBranches = new ArrayList<>();
    private List<String> robotBranchPrefixes = new ArrayList<>();
    private List<String> prEvents = new ArrayList<>();
    private String webhookCallbackUrl;

    public List<Option> getBusinessSystems()
    {
        return businessSystems;
    }

    public void setBusinessSystems(List<Option> businessSystems)
    {
        this.businessSystems = businessSystems;
    }

    public List<Option> getDepartments()
    {
        return departments;
    }

    public void setDepartments(List<Option> departments)
    {
        this.departments = departments;
    }

    public List<Option> getOwners()
    {
        return owners;
    }

    public void setOwners(List<Option> owners)
    {
        this.owners = owners;
    }

    public List<Option> getCredentials()
    {
        return credentials;
    }

    public void setCredentials(List<Option> credentials)
    {
        this.credentials = credentials;
    }

    public List<Option> getModels()
    {
        return models;
    }

    public void setModels(List<Option> models)
    {
        this.models = models;
    }

    public List<Option> getTemplates()
    {
        return templates;
    }

    public void setTemplates(List<Option> templates)
    {
        this.templates = templates;
    }

    public List<String> getLongLivedBranches()
    {
        return longLivedBranches;
    }

    public void setLongLivedBranches(List<String> longLivedBranches)
    {
        this.longLivedBranches = longLivedBranches;
    }

    public List<String> getRobotBranchPrefixes()
    {
        return robotBranchPrefixes;
    }

    public void setRobotBranchPrefixes(List<String> robotBranchPrefixes)
    {
        this.robotBranchPrefixes = robotBranchPrefixes;
    }

    public List<String> getPrEvents()
    {
        return prEvents;
    }

    public void setPrEvents(List<String> prEvents)
    {
        this.prEvents = prEvents;
    }

    public String getWebhookCallbackUrl()
    {
        return webhookCallbackUrl;
    }

    public void setWebhookCallbackUrl(String webhookCallbackUrl)
    {
        this.webhookCallbackUrl = webhookCallbackUrl;
    }

    public static class Option
    {
        private Long id;
        private String label;
        private Long deptId;
        private Long parentId;
        private String status;
        private String techStack;
        private Integer versionNo;

        public Option()
        {
        }

        public Option(Long id, String label, Long deptId, Long parentId, String status)
        {
            this.id = id;
            this.label = label;
            this.deptId = deptId;
            this.parentId = parentId;
            this.status = status;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getLabel()
        {
            return label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public Long getDeptId()
        {
            return deptId;
        }

        public void setDeptId(Long deptId)
        {
            this.deptId = deptId;
        }

        public Long getParentId()
        {
            return parentId;
        }

        public void setParentId(Long parentId)
        {
            this.parentId = parentId;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public String getTechStack()
        {
            return techStack;
        }

        public void setTechStack(String techStack)
        {
            this.techStack = techStack;
        }

        public Integer getVersionNo()
        {
            return versionNo;
        }

        public void setVersionNo(Integer versionNo)
        {
            this.versionNo = versionNo;
        }
    }
}
