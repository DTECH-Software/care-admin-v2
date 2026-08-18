package com.dtech.admin.mapper;

import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.mapper.entityToDto.DependentDetailsMapperEntityToDto;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.service.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class DependentDetailsMapperEntityToDtoTest {
    @Test
    void mapsCreateAndModificationDates() {
        DependentDetailsMapperEntityToDto mapper = new DependentDetailsMapperEntityToDto(
                new ModelMapper(), mock(DocumentStorageService.class));
        ClaimsDependents dependent = new ClaimsDependents();
        dependent.setId(1L);
        Date createdDate = new Date(1_700_000_000_000L);
        Date modificationDate = new Date(1_710_000_000_000L);
        dependent.setCreatedDate(createdDate);
        dependent.setLastModifiedDate(modificationDate);

        DependentDetailsResponseDTO response = mapper.mapDependentDetails(dependent);

        assertEquals(createdDate, response.getCreatedDate());
        assertEquals(modificationDate, response.getLastModifiedDate());
    }

    @Test
    void lightweightMappingRetainsDocumentMetadataWithoutLoadingContent() {
        DocumentStorageService documentStorageService = mock(DocumentStorageService.class);
        DependentDetailsMapperEntityToDto mapper = new DependentDetailsMapperEntityToDto(
                new ModelMapper(), documentStorageService);
        ClaimsDependents dependent = new ClaimsDependents();
        dependent.setId(1L);
        com.dtech.admin.model.Document document = new com.dtech.admin.model.Document();
        document.setFileName("birth-certificate.pdf");
        document.setFileType("application/pdf");
        dependent.setDocuments(List.of(document));

        DependentDetailsResponseDTO response = mapper.mapDependentDetailsWithoutDocumentContent(dependent);

        assertEquals("birth-certificate.pdf", response.getAttachment().getFirst().getFileName());
        assertNull(response.getAttachment().getFirst().getDoc());
        verifyNoInteractions(documentStorageService);
    }
}
