package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionPatchDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface QuestionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quiz", source = "quiz")
    @Mapping(target = "question", source = "dto.question")
    @Mapping(target = "timeInSeconds", source = "dto.timeInSeconds")
    @Mapping(target = "answers", source = "dto.answers")
    Question toEntity(QuestionCreateDto dto, Quiz quiz);

    @Mapping(target = "quizUuid", source = "quiz.uuid")
    QuestionResponse toResponse(Question question);

    @BeanMapping(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    void updateFromPatch(
        QuestionPatchDto dto,
        @MappingTarget Question question
    );
}
