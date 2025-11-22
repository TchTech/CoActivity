package com.mipt.CoActivity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "room_folders")
public class RoomFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folderName")
    private String folderName;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "folder_rooms",
            joinColumns = @JoinColumn(name = "folderId"),
            inverseJoinColumns = @JoinColumn(name = "roomId")
    )
    private List<Room> rooms;

    public RoomFolder(String folderName, User user) {
        this.folderName = folderName;
        this.user = user;
        this.rooms = new ArrayList<>();
    }
}

