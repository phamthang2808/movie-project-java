package com.example.thangcachep.movie_project_be.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(max = 1000, message = "Nội dung comment không được vượt quá 1000 ký tự")
    private String content;

    private Long parentId; // null nếu là comment gốc, có giá trị nếu là reply

    private Boolean isAnonymous; // true nếu ẩn danh, false nếu không (mặc định false)
}

