package fetch.db;

import fetch.db.model.ArticleDetailUrlModel;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;

/**
 * @author zenggs
 * @Date 2023/4/3
 */
@Transactional(rollbackOn = Exception.class)
public interface ArticleDetailUrlDao extends JpaRepository<ArticleDetailUrlModel,Long> {
}
