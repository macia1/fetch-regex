package fetch.db.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * @author zenggs
 * @Date 2023/4/3
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "crawler_machine_detail_url")
public class ArticleDetailUrlModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleDetailUrlId;
    @Column
    private String url;
    @CreatedDate
    private Date createDate;
    @LastModifiedDate
    private Date modifiedDate;

    public ArticleDetailUrlModel(String url) {
        this.url = url;
    }

    public ArticleDetailUrlModel() {

    }
}
