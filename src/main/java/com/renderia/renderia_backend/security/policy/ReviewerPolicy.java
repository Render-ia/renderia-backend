package com.renderia.renderia_backend.security.policy;

/**
 * The reviewer checks the work of others: sees every project and can mark
 * analyses as reviewed, but does not create, edit or delete projects.
 */
public class ReviewerPolicy implements RolePolicy {

    @Override
    public boolean canViewAllProjects() {
        return true;
    }

    @Override
    public boolean canManageAnyProject() {
        return false;
    }

    @Override
    public int projectLimit() {
        return 0;
    }

    @Override
    public boolean canReviewAnyProject() {
        return true;
    }
}
