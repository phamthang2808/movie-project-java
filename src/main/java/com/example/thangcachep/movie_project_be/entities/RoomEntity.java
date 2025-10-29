package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomEntity extends BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "name")
    private String name;

    @Column(name = "image")
    private String image;

    @Column(name = "address")
    private String address;

    @Column(name = "description")
    private String description;

    @Column(name = "title")
    private String title;

    @Column(name = "guests")
    private Integer guests;

    @Column(name = "size")
    private Integer size;

    @Column(name = "beds")
    private String beds;

    @Column(name = "view")
    private String view;

    @Column(name = "price")
    private Double price;

    @Column(name = "old_price")
    private Double oldPrice;

    @Column(name = "discount")
    private Integer discount;

    @Column(name = "air_conditioning")
    private Boolean airConditioning;

    private Boolean wifi;


    @Column(name = "hair_dryer")
    private Boolean hairDryer;

    @Column(name = "pets_allowed")
    private Boolean petsAllowed;

    @Column(name = "non_smoking")
    private Boolean nonSmoking;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagementRoomEntity> managementRooms;

    @OneToOne
    @JoinColumn(name = "type_id")
    private RoomTypeEntity roomType;

}
