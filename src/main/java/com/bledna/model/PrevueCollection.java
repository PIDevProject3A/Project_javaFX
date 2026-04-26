package com.bledna.model;

import javafx.beans.property.*;


public class PrevueCollection {

    private final IntegerProperty id                = new SimpleIntegerProperty();
    private final StringProperty  typeCollection    = new SimpleStringProperty();
    private final DoubleProperty  quantite          = new SimpleDoubleProperty();
    private final StringProperty  statut            = new SimpleStringProperty("PENDING");
    private final IntegerProperty wasteCollectionId = new SimpleIntegerProperty(0);
    private final ObjectProperty<java.time.LocalDateTime> collectionDate = new SimpleObjectProperty<>();
    private final StringProperty  unit                = new SimpleStringProperty("kg");

    public PrevueCollection() {}

    // ── id ──────────────────────────────────────────────────────────────────
    public int getId()           { return id.get(); }
    public void setId(int v)     { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    // ── typeCollection ───────────────────────────────────────────────────────
    public String getTypeCollection()          { return typeCollection.get(); }
    public void   setTypeCollection(String v)  { typeCollection.set(v); }
    public StringProperty typeCollectionProperty() { return typeCollection; }

    // ── quantite ─────────────────────────────────────────────────────────────
    public double getQuantite()          { return quantite.get(); }
    public void   setQuantite(double v)  { quantite.set(v); }
    public DoubleProperty quantiteProperty() { return quantite; }

    // ── statut ───────────────────────────────────────────────────────────────
    public String getStatut()          { return statut.get(); }
    public void   setStatut(String v)  { statut.set(v); }
    public StringProperty statutProperty() { return statut; }

    // ── wasteCollectionId ────────────────────────────────────────────────────
    public int  getWasteCollectionId()        { return wasteCollectionId.get(); }
    public void setWasteCollectionId(int v)   { wasteCollectionId.set(v); }
    public IntegerProperty wasteCollectionIdProperty() { return wasteCollectionId; }

    // ── collectionDate ──────────────────────────────────────────────────────
    public java.time.LocalDateTime getCollectionDate()             { return collectionDate.get(); }
    public void          setCollectionDate(java.time.LocalDateTime v) { collectionDate.set(v); }
    public ObjectProperty<java.time.LocalDateTime> collectionDateProperty() { return collectionDate; }

    // ── unit ─────────────────────────────────────────────────────────────────
    public String getUnit()          { return unit.get(); }
    public void   setUnit(String v)  { unit.set(v); }
    public StringProperty unitProperty() { return unit; }
}
