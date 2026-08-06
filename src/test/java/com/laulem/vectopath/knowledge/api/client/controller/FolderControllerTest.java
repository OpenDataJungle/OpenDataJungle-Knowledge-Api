package com.laulem.vectopath.knowledge.api.client.controller;

import com.laulem.vectopath.knowledge.api.business.model.Folder;
import com.laulem.vectopath.knowledge.api.business.service.AuthenticationUseCase;
import com.laulem.vectopath.knowledge.api.business.service.FolderUseCase;
import com.laulem.vectopath.knowledge.api.client.dto.FolderRequest;
import com.laulem.vectopath.knowledge.api.client.dto.FolderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderControllerTest {

    @Mock
    private FolderUseCase folderUseCase;

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @InjectMocks
    private FolderController controller;

    @Test
    void create_shouldBuildFolderFromRequestAndCurrentUser_andDelegateToUseCase() {
        // Given
        FolderRequest request = new FolderRequest("docs", "/root", List.of(UUID.randomUUID()));
        when(authenticationUseCase.getCurrentUser()).thenReturn("alice");
        Folder created = aFolder();
        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        when(folderUseCase.create(folderCaptor.capture())).thenReturn(created);

        // When
        FolderResponse response = controller.create(request);

        // Then
        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getName()).isEqualTo(created.getName());
        Folder captured = folderCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("docs");
        assertThat(captured.getPath()).isEqualTo("/root");
        assertThat(captured.getCreatedBy()).isEqualTo("alice");
        assertThat(captured.getGroupIds()).isEqualTo(request.groupIds());
    }

    @Test
    void listAll_shouldMapEveryFolder() {
        // Given
        Folder first = aFolder();
        Folder second = aFolder();
        when(folderUseCase.listAll()).thenReturn(List.of(first, second));

        // When
        List<FolderResponse> responses = controller.listAll();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(first.getId());
        assertThat(responses.get(1).getId()).isEqualTo(second.getId());
    }

    @Test
    void getMyDefaultFolder_shouldReturnDefaultFolderIdAndCompletePath() {
        // Given
        Folder folder = aFolder();
        when(folderUseCase.getOrCreateDefaultFolder()).thenReturn(folder);

        // When
        FolderResponse response = controller.getMyDefaultFolder();

        // Then
        assertThat(response.getId()).isEqualTo(folder.getId());
        assertThat(response.getCompletePath()).isEqualTo(folder.getCompletePath());
    }

    @Test
    void getById_shouldReturnFolderMappedToAllResponseFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, Month.JUNE, 1, 12, 30);
        Folder folder = new Folder("docs", "/root", List.of(groupId), "alice");
        folder.setId(id);
        folder.setParentId(parentId);
        folder.setCreatedAt(createdAt);
        folder.setUpdatedAt(updatedAt);
        when(folderUseCase.getById(id)).thenReturn(folder);

        // When
        FolderResponse response = controller.getById(id);

        // Then
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("docs");
        assertThat(response.getPath()).isEqualTo("/root");
        assertThat(response.getCompletePath()).isEqualTo(folder.getCompletePath());
        assertThat(response.getParentId()).isEqualTo(parentId);
        assertThat(response.getGroupIds()).isEqualTo(List.of(groupId));
        assertThat(response.getCreatedBy()).isEqualTo("alice");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void findAllChildren_shouldMapEveryChild() {
        // Given
        UUID parentId = UUID.randomUUID();
        Folder first = aFolder();
        Folder second = aFolder();
        when(folderUseCase.findAllChildren(parentId)).thenReturn(List.of(first, second));

        // When
        List<FolderResponse> responses = controller.findAllChildren(parentId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(first.getId());
        assertThat(responses.get(1).getId()).isEqualTo(second.getId());
    }

    @Test
    void update_shouldSetIdNameAndPathAndGroupIds_beforeDelegating() {
        // Given
        UUID id = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        FolderRequest request = new FolderRequest("renamed", "/new-path", List.of(groupId));
        Folder updated = aFolder();
        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        when(folderUseCase.update(folderCaptor.capture())).thenReturn(updated);

        // When
        FolderResponse response = controller.update(id, request);

        // Then
        assertThat(response.getId()).isEqualTo(updated.getId());
        Folder captured = folderCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(id);
        assertThat(captured.getName()).isEqualTo("renamed");
        assertThat(captured.getPath()).isEqualTo("/new-path");
        assertThat(captured.getGroupIds()).isEqualTo(List.of(groupId));
        assertThat(captured.getCreatedBy()).isNull();
    }

    @Test
    void delete_shouldDelegateToUseCase() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        controller.delete(id);

        // Then
        verify(folderUseCase).delete(id);
    }

    private static Folder aFolder() {
        Folder folder = new Folder("docs", "/root", List.of(), "alice");
        folder.setId(UUID.randomUUID());
        return folder;
    }
}
