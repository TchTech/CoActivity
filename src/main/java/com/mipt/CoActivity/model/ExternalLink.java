package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "external_links")
public class ExternalLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @Column(name = "platformName")
    private String platformName;

    @Column(name = "url")
    private String url;

    public ExternalLink(User user, String platformName, String url) {
        this.user = user;
        this.platformName = platformName;
        this.url = url;
    }
}

