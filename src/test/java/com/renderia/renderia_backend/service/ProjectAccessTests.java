package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.renderia.renderia_backend.dto.ProjectMemberRequest;
import com.renderia.renderia_backend.dto.ProjectRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.model.ProjectMember;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.BuildingTypeRepository;
import com.renderia.renderia_backend.repository.ProjectMemberRepository;
import com.renderia.renderia_backend.repository.ProjectRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.support.TestUsers;

/** Who can see, change and share a project, for each of the five roles. */
class ProjectAccessTests {

    private static final long OWNER = 1L;
    private static final long PROJECT = 10L;

    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private UserRepository users;
    private ProjectService service;
    private ProjectMemberService sharing;
    private Project project;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        users = mock(UserRepository.class);
        service = new ProjectService(projects, mock(BuildingTypeRepository.class), members);
        sharing = new ProjectMemberService(service, members, users);

        project = new Project();
        ReflectionTestUtils.setField(project, "id", PROJECT);
        project.setUserId(OWNER);
        when(projects.findById(PROJECT)).thenReturn(Optional.of(project));
        when(members.save(any(ProjectMember.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void clientSeesOnlyProjectsSharedWithThem() {
        CurrentUser client = new CurrentUser(2L, Role.CLIENT);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> service.findVisible(client, PROJECT)).getStatus());

        when(members.existsByProjectIdAndUserId(PROJECT, 2L)).thenReturn(true);
        assertSame(project, service.findVisible(client, PROJECT));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> service.findManageable(client, PROJECT)).getStatus());
    }

    @Test
    void reviewerSeesEverythingButCannotChangeIt() {
        CurrentUser reviewer = new CurrentUser(3L, Role.REVIEWER);
        assertSame(project, service.findVisible(reviewer, PROJECT));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> service.findManageable(reviewer, PROJECT)).getStatus());
    }

    @Test
    void reviewerAndClientCannotCreateProjects() {
        ProjectRequest request = new ProjectRequest("Casa", null, null, null);
        for (String role : new String[] {Role.REVIEWER, Role.CLIENT}) {
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                    () -> service.create(new CurrentUser(4L, role), request)).getStatus());
        }
        verify(projects, never()).save(any());
    }

    @Test
    void realAdminManagesAnyProjectButTheDemoAdminDoesNot() {
        assertSame(project, service.findManageable(new CurrentUser(5L, Role.ADMIN), PROJECT));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> service.findManageable(new CurrentUser(6L, Role.ADMIN, true), PROJECT)).getStatus());
    }

    @Test
    void ownerSharesWithAnActiveUserOnce() {
        User client = TestUsers.user(2L, Role.CLIENT, true);
        when(users.findByEmailIgnoreCase("usuario2@renderia.co")).thenReturn(Optional.of(client));
        CurrentUser owner = new CurrentUser(OWNER, Role.ENGINEER);

        assertEquals("usuario2@renderia.co",
                sharing.add(owner, PROJECT, new ProjectMemberRequest(" Usuario2@renderia.co ")).email());

        when(members.existsByProjectIdAndUserId(PROJECT, 2L)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> sharing.add(owner, PROJECT, new ProjectMemberRequest("usuario2@renderia.co"))).getStatus());
    }

    @Test
    void sharingRejectsTheOwnerInactiveUsersAndStrangers() {
        CurrentUser owner = new CurrentUser(OWNER, Role.ENGINEER);
        when(users.findByEmailIgnoreCase("usuario1@renderia.co"))
                .thenReturn(Optional.of(TestUsers.user(OWNER, Role.ENGINEER, true)));
        when(users.findByEmailIgnoreCase("usuario8@renderia.co"))
                .thenReturn(Optional.of(TestUsers.user(8L, Role.CLIENT, false)));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> sharing.add(owner, PROJECT, new ProjectMemberRequest("usuario1@renderia.co"))).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
                () -> sharing.add(owner, PROJECT, new ProjectMemberRequest("usuario8@renderia.co"))).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> sharing.add(new CurrentUser(9L, Role.REVIEWER), PROJECT,
                        new ProjectMemberRequest("usuario8@renderia.co"))).getStatus());
        verify(members, never()).save(any());
    }
}
