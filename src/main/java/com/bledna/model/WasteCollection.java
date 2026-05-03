package com.bledna.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;


public class WasteCollection {

    private final IntegerProperty id             = new SimpleIntegerProperty();
    private final IntegerProperty collectorId    = new SimpleIntegerProperty(1); // static user
    private final StringProperty  locationName   = new SimpleStringProperty();
    private final StringProperty  wasteType      = new SimpleStringProperty();
    private final DoubleProperty  quantity        = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDateTime> collectionDate = new SimpleObjectProperty<>();
    private final StringProperty  gpsLocation    = new SimpleStringProperty();
    private final StringProperty  status         = new SimpleStringProperty("PENDING");
    private final StringProperty  description    = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt  = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt  = new SimpleObjectProperty<>();
    private final StringProperty  unit           = new SimpleStringProperty("kg");
    private final StringProperty  imagePath      = new SimpleStringProperty();

    public WasteCollection() {}

    // ── id ──────────────────────────────────────────────────────────────────
    public int getId()              { return id.get(); }
    public void setId(int v)        { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    // ── collectorId ─────────────────────────────────────────────────────────
    public int getCollectorId()        { return collectorId.get(); }
    public void setCollectorId(int v)  { collectorId.set(v); }

    // ── locationName ────────────────────────────────────────────────────────
    public String getLocationName()           { return locationName.get(); }
    public void   setLocationName(String v)   { locationName.set(v); }
    public StringProperty locationNameProperty() { return locationName; }

    // ── wasteType ───────────────────────────────────────────────────────────
    public String getWasteType()          { return wasteType.get(); }
    public void   setWasteType(String v)  { wasteType.set(v); }
    public StringProperty wasteTypeProperty() { return wasteType; }

    // ── quantity ────────────────────────────────────────────────────────────
    public double getQuantity()          { return quantity.get(); }
    public void   setQuantity(double v)  { quantity.set(v); }
    public DoubleProperty quantityProperty() { return quantity; }

    // ── collectionDate ──────────────────────────────────────────────────────
    public LocalDateTime getCollectionDate()             { return collectionDate.get(); }
    public void          setCollectionDate(LocalDateTime v) { collectionDate.set(v); }
    public ObjectProperty<LocalDateTime> collectionDateProperty() { return collectionDate; }

    // ── gpsLocation ─────────────────────────────────────────────────────────
    public String getGpsLocation()          { return gpsLocation.get(); }
    public void   setGpsLocation(String v)  { gpsLocation.set(v); }

    // ── status ───────────────────────────────────────────────────────────────
    public String getStatus()          { return status.get(); }
    public void   setStatus(String v)  { status.set(v); }
    public StringProperty statusProperty() { return status; }

    // ── description ──────────────────────────────────────────────────────────
    public String getDescription()          { return description.get(); }
    public void   setDescription(String v)  { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    // ── createdAt ────────────────────────────────────────────────────────────
    public LocalDateTime getCreatedAt()             { return createdAt.get(); }
    public void          setCreatedAt(LocalDateTime v) { createdAt.set(v); }

    // ── updatedAt ────────────────────────────────────────────────────────────
    public LocalDateTime getUpdatedAt()             { return updatedAt.get(); }
    public void          setUpdatedAt(LocalDateTime v) { updatedAt.set(v); }

    // ── unit ─────────────────────────────────────────────────────────────────
    public String getUnit()          { return unit.get(); }
    public void   setUnit(String v)  { unit.set(v); }
    public StringProperty unitProperty() { return unit; }

    // ── imagePath ────────────────────────────────────────────────────────────
    public String getImagePath()          { return imagePath.get(); }
    public void   setImagePath(String v)  { imagePath.set(v); }
    public StringProperty imagePathProperty() { return imagePath; }

    @Override
    public String toString() {
        return "#" + getId() + " — " + getLocationName();
    }
}
