package com.eventsphere.entity;

import jakarta.persistence.*;

/**
 * Speakers are master data shared by all organizers; they do not log in.
 */
@Entity
@Table(name = "speakers")
public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(length = 160)
    private String organization;

    @Column(length = 120)
    private String designation;

    @Column(length = 2000)
    private String bio;

    @Column(length = 300)
    private String expertise;

    protected Speaker() {
    }

    public Speaker(String fullName, String email, String organization, String designation, String bio, String expertise) {
        this.fullName = fullName;
        this.email = email;
        this.organization = organization;
        this.designation = designation;
        this.bio = bio;
        this.expertise = expertise;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
}
