package hexlet.code.app.mapper;


import hexlet.code.app.dto.UserCreateDTO;
import hexlet.code.app.dto.UserDTO;
import hexlet.code.app.dto.UserUpdateDTO;
import hexlet.code.app.model.User;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {
//    @Mapping(target = "category.id", source = "categoryId")
    //@Mapping(target = "category.name", source = "categoryName")
    public abstract User map(UserCreateDTO dto);

//    @Mapping(source = "category.id", target = "categoryId")
//    @Mapping(source = "category.name", target = "categoryName")
    public abstract UserDTO map(User model);

//    @Mapping(target = "category.id", source = "categoryId")
    public abstract void update(UserUpdateDTO dto, @MappingTarget User model);
}


