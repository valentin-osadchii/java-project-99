package hexlet.code.app.service;

import hexlet.code.app.dto.LabelCreateDTO;
import hexlet.code.app.dto.LabelDTO;
import hexlet.code.app.dto.LabelUpdateDTO;
import hexlet.code.app.model.Label;

import java.util.List;

public interface LabelService {

    List<LabelDTO> getAll();

    LabelDTO getById(long id);

    LabelDTO create(LabelCreateDTO labelData);

    LabelDTO update(long id, LabelUpdateDTO labelData);

    void delete(long id);

    Label findByName(String name);
}
