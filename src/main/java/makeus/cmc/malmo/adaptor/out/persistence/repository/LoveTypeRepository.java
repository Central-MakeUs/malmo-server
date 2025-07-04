package makeus.cmc.malmo.adaptor.out.persistence.repository;

import makeus.cmc.malmo.adaptor.out.persistence.entity.love_type.LoveTypeCategoryJpa;
import makeus.cmc.malmo.adaptor.out.persistence.entity.love_type.LoveTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoveTypeRepository extends JpaRepository<LoveTypeEntity, Long> {

    Optional<LoveTypeEntity> findByLoveTypeCategoryJpa(LoveTypeCategoryJpa loveTypeCategory);
}
