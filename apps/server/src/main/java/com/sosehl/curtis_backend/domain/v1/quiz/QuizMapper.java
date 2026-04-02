package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.question.QuestionMapper;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = { QuestionMapper.class })
public interface QuizMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "questions", ignore = true)
    Quiz toEntity(QuizCreateRequest request);

    QuizGetResponse toResponse(Quiz quiz);

    @BeanMapping(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "questions", ignore = true)
    void updateFromPatch(QuizPatchRequest request, @MappingTarget Quiz quiz);
}
