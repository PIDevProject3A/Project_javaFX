package com.esprit.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.esprit.utils.MyDataBase;
import com.esprit.utils.SceneNavigator;
import com.esprit.services.RecyclingBuyerDao;
import com.esprit.entities.RecyclingBuyer;
import com.esprit.entities.WeatherData;
import com.esprit.services.WeatherService;
import io.github.cdimascio.dotenv.Dotenv;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class GpsController {

    private static final ObservableList<String> RECYCLING_TYPES = FXCollections.observableArrayList(List.of(
        "Plastic",
        "Metal",
        "Glass",
        "Paper",
        "Mixed",
        "Organic"
    ));

    private static final ObservableList<String> BUYER_STATUSES = FXCollections.observableArrayList(List.of(
        "Active",
        "Temporarily Closed",
        "Inactive"
    ));

        private static final String MAP_HTML_TEMPLATE = """
        <!doctype html>
        <html>
        <head>
          <meta charset=\"utf-8\" />
          <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" />
          <title>Bledna Buyers Map</title>
          <style>
                        html, body, #map { height: 100%; margin: 0; }
            body { font-family: Arial, sans-serif; }
                        .leaflet-pane,
                        .leaflet-tile,
                        .leaflet-marker-icon,
                        .leaflet-marker-shadow {
                            transition: none !important;
                            animation: none !important;
                        }
                        #fallback {
                            display: none;
                            padding: 14px;
                            color: #14532d;
                            font-size: 14px;
                            line-height: 1.4;
                        }
          </style>
        </head>
        <body>
          <div id=\"map\"></div>
                    <div id=\"fallback\">Loading map...</div>
          <script>
                        (function() {
                        let map = null;
                        let buyersDataSource = null;
                        let draftMarker = null;
                        let popup = null;
                        let fallbackMode = null;
                        let leafletMap = null;
                        let leafletMarkersLayer = null;
                        let leafletDraftMarker = null;
                        let azureKey = null;
                        let leafletLoaded = false;

                        window.renderMarkers = function() {};
                        window.setDraftMarker = function() {};

                        function showFallback(message) {
                            document.getElementById('map').style.display = 'none';
                            const fallback = document.getElementById('fallback');
                            fallback.textContent = message;
                            fallback.style.display = 'block';
                        }

                        function notifyJavaMapClicked(lat, lng) {
                            if (!window.javaBridge) {
                                return;
                            }

                            try {
                                window.javaBridge.onMapClicked(lat, lng);
                            } catch (e) {
                                // Keep JS map interactions working even if Java bridge is not ready yet.
                            }
                        }

                        window.onerror = function(message, source, lineno, colno) {
                            const msg = message == null ? '' : String(message);
                            const src = source == null ? '' : String(source);

                            // Cross-origin script failures often surface as "Script error." with no location.
                            // Don't blank the map UI for that generic browser-level error.
                            if ((msg === 'Script error.' || msg === 'Script error') && !src && (!lineno || lineno === 0)) {
                                return true;
                            }

                            showFallback('Map runtime error: ' + msg);
                            return true;
                        };

            function esc(text) {
                            const safe = text == null ? '' : String(text);
                            return safe.replace(/[&<>\"']/g, function(s) {
                                return {'&':'&amp;','<':'&lt;','>':'&gt;','\\\"':'&quot;',"'":'&#39;'}[s];
                            });
            }

            window.renderMarkers = function(markers) {
                            if (fallbackMode === 'leaflet') {
                                if (!leafletMap || !leafletMarkersLayer) {
                                    return;
                                }

                                leafletMarkersLayer.clearLayers();
                                if (!Array.isArray(markers) || markers.length === 0) {
                                    leafletMap.setView([34.0, 9.0], 6);
                                    return;
                                }

                                const bounds = [];
                                markers.forEach(m => {
                                    const lat = Number(m.lat);
                                    const lng = Number(m.lng);
                                    if (Number.isNaN(lat) || Number.isNaN(lng)) {
                                        return;
                                    }

                                    const popupHtml = `
                                        <div style=\"min-width:190px;line-height:1.35;\">
                                            <strong>${esc(m.name)}</strong><br/>
                                            Type: ${esc(m.type)}<br/>
                                            City: ${esc(m.city)}<br/>
                                            Status: ${esc(m.status)}<br/>
                                            Address: ${esc(m.address)}
                                        </div>
                                    `;

                                    L.circleMarker([lat, lng], {
                                        radius: 6,
                                        color: '#1d4ed8',
                                        weight: 2,
                                        fillColor: '#3b82f6',
                                        fillOpacity: 0.9
                                    }).addTo(leafletMarkersLayer).bindPopup(popupHtml);
                                    bounds.push([lat, lng]);
                                });

                                if (bounds.length === 1) {
                                    leafletMap.setView(bounds[0], 12);
                                } else if (bounds.length > 1) {
                                    leafletMap.fitBounds(bounds, { padding: [30, 30] });
                                }
                                return;
                            }

                            if (!map || !buyersDataSource) {
                                return;
                            }

                            buyersDataSource.clear();

              if (!Array.isArray(markers) || markers.length === 0) {
                                map.setCamera({ center: [9.0, 34.0], zoom: 6 });
                return;
              }

                            const bounds = [];
              markers.forEach(m => {
                const lat = Number(m.lat);
                const lng = Number(m.lng);
                if (Number.isNaN(lat) || Number.isNaN(lng)) {
                  return;
                }

                                const point = new atlas.data.Point([lng, lat]);
                                const shape = new atlas.Shape(
                                    new atlas.data.Feature(point, {
                                        name: m.name,
                                        type: m.type,
                                        city: m.city,
                                        status: m.status,
                                        address: m.address
                                    })
                                );

                                buyersDataSource.add(shape);
                                bounds.push([lng, lat]);
              });

                            if (bounds.length === 1) {
                                map.setCamera({ center: bounds[0], zoom: 12 });
                            } else if (bounds.length > 1) {
                                map.setCamera({ bounds: atlas.data.BoundingBox.fromPositions(bounds), padding: 60 });
              }
            };

                        window.setDraftMarker = function(lat, lng) {
                            if (fallbackMode === 'leaflet') {
                                if (!leafletMap) {
                                    return;
                                }

                                if (leafletDraftMarker) {
                                    leafletMap.removeLayer(leafletDraftMarker);
                                    leafletDraftMarker = null;
                                }

                                if (typeof lat !== 'number' || typeof lng !== 'number' || Number.isNaN(lat) || Number.isNaN(lng)) {
                                    return;
                                }

                                leafletDraftMarker = L.circleMarker([lat, lng], {
                                    radius: 7,
                                    color: '#b91c1c',
                                    weight: 2,
                                    fillColor: '#ef4444',
                                    fillOpacity: 1
                                }).addTo(leafletMap).bindPopup('Selected location');
                                leafletMap.setView([lat, lng], 13);
                                return;
                            }

                            if (!map) {
                                return;
                            }

                            if (draftMarker) {
                                map.markers.remove(draftMarker);
                                draftMarker = null;
                            }

                            if (typeof lat !== 'number' || typeof lng !== 'number' || Number.isNaN(lat) || Number.isNaN(lng)) {
                                return;
                            }

                            draftMarker = new atlas.HtmlMarker({
                                position: [lng, lat],
                                color: '#dc2626',
                                text: 'S'
                            });

                            map.markers.add(draftMarker);
                            map.setCamera({ center: [lng, lat], zoom: 13 });
                        };

                        function openPopupForShape(shape) {
                            if (!shape) {
                                return;
                            }

                            const props = shape.getProperties ? shape.getProperties() : null;
                            const coords = shape.getCoordinates ? shape.getCoordinates() : null;
                            if (!props || !coords || coords.length !== 2) {
                                return;
                            }

                            const popupHtml = `
                                <div style=\"min-width:190px;line-height:1.35;\">
                                    <strong>${esc(props.name)}</strong><br/>
                                    Type: ${esc(props.type)}<br/>
                                    City: ${esc(props.city)}<br/>
                                    Status: ${esc(props.status)}<br/>
                                    Address: ${esc(props.address)}
                                </div>
                            `;

                            popup.setOptions({
                                content: popupHtml,
                                position: coords
                            });
                            popup.open(map);
                        }

                        function initializeAzureMap() {
                            if (!window.atlas) {
                                showFallback('Azure Maps script loaded incorrectly. Reopen GPS tab and try again.');
                                return;
                            }

                            const key = '{{AZURE_MAPS_API_KEY}}';
                            azureKey = key;
                            if (!key || key === 'MISSING_KEY') {
                                showFallback('Azure Maps API key is missing. Add map.azure.apiKey in db.properties or set AZURE_MAPS_KEY env var.');
                                return;
                            }

                            try {
                                map = new atlas.Map('map', {
                                    center: [9.0, 34.0],
                                    zoom: 6,
                                    language: 'fr-FR',
                                    authOptions: {
                                        authType: 'subscriptionKey',
                                        subscriptionKey: key
                                    }
                                });
                            } catch (err) {
                                const msg = err ? String(err) : 'Unknown map initialization error.';
                                if (msg.indexOf('WebGL') >= 0) {
                                    initializeLeafletAzureFallback('WebGL is unavailable in this JavaFX WebView.');
                                    return;
                                }
                                showFallback('Azure Maps init failed: ' + msg);
                                return;
                            }

                            map.events.add('error', function(e) {
                                const message = e && e.error ? String(e.error) : 'Unknown Azure Maps error.';
                                if (message.indexOf('WebGL') >= 0) {
                                    initializeLeafletAzureFallback('WebGL is unavailable in this JavaFX WebView.');
                                    return;
                                }
                                showFallback('Azure Maps error: ' + message);
                            });

                            map.events.add('ready', function() {
                                document.getElementById('fallback').style.display = 'none';
                                document.getElementById('map').style.display = 'block';

                                buyersDataSource = new atlas.source.DataSource();
                                map.sources.add(buyersDataSource);

                                const buyersLayer = new atlas.layer.SymbolLayer(buyersDataSource, null, {
                                    iconOptions: {
                                        image: 'pin-round-blue',
                                        allowOverlap: true,
                                        ignorePlacement: true
                                    }
                                });
                                map.layers.add(buyersLayer);

                                popup = new atlas.Popup({ closeButton: true });

                                map.events.add('click', buyersLayer, function(e) {
                                    if (e.shapes && e.shapes.length > 0) {
                                        openPopupForShape(e.shapes[0]);
                                    }
                                });

                                map.events.add('click', function(e) {
                                    if (!e || !e.position) {
                                        return;
                                    }

                                    const clickPosition = map.pixelToPosition(e.position);
                                    if (!clickPosition || clickPosition.length !== 2) {
                                        return;
                                    }

                                    const lng = Number(clickPosition[0].toFixed(6));
                                    const lat = Number(clickPosition[1].toFixed(6));

                                    window.setDraftMarker(lat, lng);
                                    notifyJavaMapClicked(lat, lng);
                                });

                                window.renderMarkers(window.__lastMarkers || []);
                                if (window.__draftLocation) {
                                    window.setDraftMarker(window.__draftLocation.lat, window.__draftLocation.lng);
                                }
                            });
                        }

                        function initializeLeafletAzureFallback(reason) {
                            if (fallbackMode === 'leaflet') {
                                return;
                            }

                            if (!azureKey || azureKey === 'MISSING_KEY') {
                                showFallback('Fallback map cannot start because Azure key is missing.');
                                return;
                            }

                            const startLeaflet = function() {
                                fallbackMode = 'leaflet';
                                document.getElementById('fallback').style.display = 'none';
                                document.getElementById('map').style.display = 'block';

                                if (!leafletMap) {
                                    if (window.L && L.Browser) {
                                        L.Browser.any3d = false;
                                    }

                                    leafletMap = L.map('map', {
                                        zoomAnimation: false,
                                        fadeAnimation: false,
                                        markerZoomAnimation: false,
                                        inertia: false,
                                        preferCanvas: true,
                                        zoomSnap: 1,
                                        zoomDelta: 1
                                    }).setView([34.0, 9.0], 6);
                                    const tileUrl = 'https://atlas.microsoft.com/map/tile?api-version=2.1&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&tileSize=256&language=fr-FR&subscription-key=' + encodeURIComponent(azureKey);

                                    L.tileLayer(tileUrl, {
                                        maxZoom: 19,
                                        attribution: '&copy; Azure Maps',
                                        updateWhenIdle: true,
                                        updateWhenZooming: false,
                                        updateInterval: 300,
                                        keepBuffer: 1,
                                        detectRetina: false
                                    }).addTo(leafletMap);

                                    leafletMarkersLayer = L.layerGroup().addTo(leafletMap);

                                    leafletMap.on('click', function(e) {
                                        const lat = Number(e.latlng.lat.toFixed(6));
                                        const lng = Number(e.latlng.lng.toFixed(6));

                                        window.setDraftMarker(lat, lng);
                                        notifyJavaMapClicked(lat, lng);
                                    });
                                }

                                window.renderMarkers(window.__lastMarkers || []);
                                if (window.__draftLocation) {
                                    window.setDraftMarker(window.__draftLocation.lat, window.__draftLocation.lng);
                                }
                            };

                            const loadLeafletAssets = function() {
                                if (leafletLoaded && window.L) {
                                    startLeaflet();
                                    return;
                                }

                                const css = document.createElement('link');
                                css.rel = 'stylesheet';
                                css.href = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css';
                                css.onerror = function() {
                                    css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
                                };
                                document.head.appendChild(css);

                                const scriptSources = [
                                    'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js',
                                    'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
                                ];

                                let idx = 0;
                                const tryLoad = function() {
                                    if (idx >= scriptSources.length) {
                                        showFallback('Map fallback failed: Leaflet assets could not be loaded.');
                                        return;
                                    }

                                    const script = document.createElement('script');
                                    script.src = scriptSources[idx++];
                                    script.async = true;
                                    script.defer = true;
                                    script.onload = function() {
                                        leafletLoaded = true;
                                        startLeaflet();
                                    };
                                    script.onerror = tryLoad;
                                    document.head.appendChild(script);
                                };

                                tryLoad();
                            };

                            loadLeafletAssets();
                        }

                        function loadAzureMapsAssets() {
                            const css = document.createElement('link');
                            css.rel = 'stylesheet';
                            css.href = 'https://atlas.microsoft.com/sdk/javascript/mapcontrol/2/atlas.min.css';
                            css.onerror = function() {
                                showFallback('Azure Maps stylesheet could not be downloaded. Check network/proxy/firewall and reopen GPS tab.');
                            };
                            document.head.appendChild(css);

                            const script = document.createElement('script');
                            script.src = 'https://atlas.microsoft.com/sdk/javascript/mapcontrol/2/atlas.min.js';
                            script.async = true;
                            script.defer = true;
                            script.onload = initializeAzureMap;
                            script.onerror = function() {
                                showFallback('Azure Maps SDK could not be downloaded. Check network/proxy/firewall and reopen GPS tab.');
                            };
                            document.head.appendChild(script);
                        }

                        const originalRenderMarkers = window.renderMarkers;
                        window.renderMarkers = function(markers) {
                            window.__lastMarkers = markers;
                            originalRenderMarkers(markers);
                        };

                        const originalSetDraftMarker = window.setDraftMarker;
                        window.setDraftMarker = function(lat, lng) {
                            window.__draftLocation = (typeof lat === 'number' && typeof lng === 'number') ? { lat, lng } : null;
                            originalSetDraftMarker(lat, lng);
                        };

                        setTimeout(function() {
                            if (!map && fallbackMode !== 'leaflet') {
                                initializeLeafletAzureFallback('Azure Web SDK load timeout.');
                            }
                        }, 12000);

                        loadAzureMapsAssets();
            })();
          </script>
        </body>
        </html>
        """;

    @FXML
    private TableView<RecyclingBuyer> buyerTable;
    @FXML
    private TableColumn<RecyclingBuyer, Integer> idColumn;
    @FXML
    private TableColumn<RecyclingBuyer, String> buyerNameColumn;
    @FXML
    private TableColumn<RecyclingBuyer, String> recyclingTypeColumn;
    @FXML
    private TableColumn<RecyclingBuyer, String> cityColumn;
    @FXML
    private TableColumn<RecyclingBuyer, String> coordinatesColumn;
    @FXML
    private TableColumn<RecyclingBuyer, String> statusColumn;

    @FXML
    private TextField buyerNameField;
    @FXML
    private ComboBox<String> recyclingTypeCombo;
    @FXML
    private TextField addressField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label selectedLocationLabel;

    @FXML
    private Label summaryLabel;
    @FXML
    private Label formStatusLabel;
    @FXML
    private Label weatherHeaderLabel;
    @FXML
    private Label weatherMetaLabel;
    @FXML
    private Label weatherTempLabel;
    @FXML
    private Label weatherWindLabel;
    @FXML
    private Label weatherRainLabel;
    @FXML
    private Label weatherStatusLabel;
    @FXML
    private ProgressBar weatherComfortBar;
    @FXML
    private ProgressBar weatherWindRiskBar;
    @FXML
    private ProgressBar weatherRainRiskBar;
    @FXML
    private Label weatherComfortValueLabel;
    @FXML
    private Label weatherWindRiskValueLabel;
    @FXML
    private Label weatherRainRiskValueLabel;
    @FXML
    private WebView mapWebView;

    private final ObservableList<RecyclingBuyer> buyers = FXCollections.observableArrayList();
    private final RecyclingBuyerDao buyerDao = new RecyclingBuyerDao();
    private final WeatherService weatherService = new WeatherService();
    private Double selectedLatitude;
    private Double selectedLongitude;

    @FXML
    private void initialize() {
        configureTable();
        configureFormOptions();
        bindSelectionToForm();
        initializeMap();
        setDefaultFormValues();

        try {
            MyDataBase.getInstance();
            loadBuyers();
            formStatusLabel.setText("Connected to database. GPS buyers loaded.");
        } catch (SQLException ex) {
            formStatusLabel.setText("Database error: " + ex.getMessage());
        }

        refreshSummary();
        updateMapMarkers();
        resetWeatherCard();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        buyerNameColumn.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        recyclingTypeColumn.setCellValueFactory(new PropertyValueFactory<>("recyclingType"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        coordinatesColumn.setCellValueFactory(cellData -> {
            RecyclingBuyer buyer = cellData.getValue();
            return new ReadOnlyStringWrapper(String.format("%.6f, %.6f", buyer.getLatitude(), buyer.getLongitude()));
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        buyerTable.setItems(buyers);
    }

    private void configureFormOptions() {
        recyclingTypeCombo.setItems(FXCollections.observableArrayList(RECYCLING_TYPES));
        statusCombo.setItems(FXCollections.observableArrayList(BUYER_STATUSES));
    }

    private void bindSelectionToForm() {
        buyerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                return;
            }

            buyerNameField.setText(newSelection.getBuyerName());
            recyclingTypeCombo.setValue(newSelection.getRecyclingType());
            addressField.setText(newSelection.getAddress());
            cityField.setText(newSelection.getCity());
            selectedLatitude = newSelection.getLatitude();
            selectedLongitude = newSelection.getLongitude();
            phoneField.setText(newSelection.getContactPhone());
            statusCombo.setValue(newSelection.getStatus());
            notesArea.setText(newSelection.getNotes());
            updateSelectedLocationLabel();
            setDraftMarkerOnMap(selectedLatitude, selectedLongitude);
            fetchWeatherForSelectedPoint();
            formStatusLabel.setText("Selected buyer ID " + newSelection.getId() + ". You can now update or delete it.");
        });
    }

    private void initializeMap() {
        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        String html = MAP_HTML_TEMPLATE.replace("{{AZURE_MAPS_API_KEY}}", resolveAzureMapsApiKey());

        try {
            Path tempHtml = Files.createTempFile("bledna-azure-map-", ".html");
            Files.writeString(tempHtml, html, StandardCharsets.UTF_8);
            tempHtml.toFile().deleteOnExit();
            engine.load(tempHtml.toUri().toString());
        } catch (IOException ex) {
            formStatusLabel.setText("Map temp file fallback failed. Loading inline map content.");
            engine.loadContent(html, "text/html");
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new MapBridge());
                updateMapMarkers();
                setDraftMarkerOnMap(selectedLatitude, selectedLongitude);
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                formStatusLabel.setText("Map WebView failed to load page content.");
            }
        });
    }

    @FXML
    private void createBuyer() {
        syncCoordinatesFromWebView();
        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        RecyclingBuyer buyer = new RecyclingBuyer(
            0,
            buyerNameField.getText().trim(),
            recyclingTypeCombo.getValue(),
            addressField.getText().trim(),
            cityField.getText().trim(),
            selectedLatitude,
            selectedLongitude,
            normalizeOptional(phoneField.getText()),
            statusCombo.getValue(),
            normalizeOptional(notesArea.getText())
        );

        try {
            RecyclingBuyer inserted = buyerDao.insert(buyer);
            buyers.add(inserted);
            refreshTableAndMap();
            buyerTable.getSelectionModel().select(inserted);
            formStatusLabel.setText("Buyer location created successfully.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to create buyer location: " + ex.getMessage());
        }
    }

    @FXML
    private void updateBuyer() {
        RecyclingBuyer selected = buyerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a buyer location from the table before updating.");
            return;
        }

        syncCoordinatesFromWebView();
        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        selected.setBuyerName(buyerNameField.getText().trim());
        selected.setRecyclingType(recyclingTypeCombo.getValue());
        selected.setAddress(addressField.getText().trim());
        selected.setCity(cityField.getText().trim());
        selected.setLatitude(selectedLatitude);
        selected.setLongitude(selectedLongitude);
        selected.setContactPhone(normalizeOptional(phoneField.getText()));
        selected.setStatus(statusCombo.getValue());
        selected.setNotes(normalizeOptional(notesArea.getText()));

        try {
            buyerDao.update(selected);
            refreshTableAndMap();
            formStatusLabel.setText("Buyer location ID " + selected.getId() + " updated.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to update buyer location: " + ex.getMessage());
        }
    }

    @FXML
    private void deleteBuyer() {
        RecyclingBuyer selected = buyerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a buyer location from the table before deleting.");
            return;
        }

        int deletedId = selected.getId();
        try {
            buyerDao.deleteById(deletedId);
            buyers.remove(selected);
            buyerTable.getSelectionModel().clearSelection();
            clearFormValuesOnly();
            refreshTableAndMap();
            formStatusLabel.setText("Buyer location ID " + deletedId + " deleted.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to delete buyer location: " + ex.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        buyerTable.getSelectionModel().clearSelection();
        clearFormValuesOnly();
        formStatusLabel.setText("Form cleared.");
    }

    @FXML
    private void reloadFromDatabase() {
        try {
            loadBuyers();
            refreshTableAndMap();
            formStatusLabel.setText("GPS buyers reloaded from database.");
        } catch (SQLException ex) {
            formStatusLabel.setText("Failed to reload buyers: " + ex.getMessage());
        }
    }

    @FXML
    private void openDonationTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/donation.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openTransactionTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/transaction.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openDashboardTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/Dashboard.fxml", err -> System.err.println(err));
    }

    private void loadBuyers() throws SQLException {
        buyers.setAll(buyerDao.findAll());
    }

    private void clearFormValuesOnly() {
        buyerNameField.clear();
        recyclingTypeCombo.getSelectionModel().selectFirst();
        addressField.clear();
        cityField.clear();
        selectedLatitude = null;
        selectedLongitude = null;
        phoneField.clear();
        statusCombo.getSelectionModel().selectFirst();
        notesArea.clear();
        updateSelectedLocationLabel();
        setDraftMarkerOnMap(null, null);
        resetWeatherCard();
    }

    private void setDefaultFormValues() {
        recyclingTypeCombo.getSelectionModel().selectFirst();
        statusCombo.getSelectionModel().selectFirst();
    }

    private ValidationResult validateForm() {
        if (buyerNameField.getText() == null || buyerNameField.getText().trim().isEmpty()) {
            return new ValidationResult(false, "Buyer name is required.");
        }

        if (recyclingTypeCombo.getValue() == null) {
            return new ValidationResult(false, "Recycling type is required.");
        }

        if (addressField.getText() == null || addressField.getText().trim().isEmpty()) {
            return new ValidationResult(false, "Address is required.");
        }

        if (cityField.getText() == null || cityField.getText().trim().isEmpty()) {
            return new ValidationResult(false, "City is required.");
        }

        if (statusCombo.getValue() == null) {
            return new ValidationResult(false, "Status is required.");
        }

        if (selectedLatitude == null || selectedLongitude == null) {
            return new ValidationResult(false, "Please click on the map to pin the buyer location.");
        }

        return new ValidationResult(true, "OK");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveAzureMapsApiKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String envKey = normalizeOptional(dotenv.get("AZURE_MAPS_KEY"));
        if (envKey != null) {
            return envKey;
        }

        Properties properties = new Properties();
        try (InputStream input = GpsController.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
                String propertyKey = normalizeOptional(properties.getProperty("map.azure.apiKey"));
                if (propertyKey != null) {
                    return propertyKey;
                }
            }
        } catch (IOException ignored) {
            // If config cannot be read, fallback marker key keeps map message explicit in UI.
        }

        return "MISSING_KEY";
    }

    private void refreshTableAndMap() {
        buyerTable.refresh();
        refreshSummary();
        updateMapMarkers();
    }

    private void refreshSummary() {
        long activeCount = buyers.stream().filter(b -> "Active".equals(b.getStatus())).count();
        Set<String> uniqueCities = new LinkedHashSet<>();
        for (RecyclingBuyer buyer : buyers) {
            uniqueCities.add(buyer.getCity());
        }

        summaryLabel.setText(String.format(
            "Buyers: %d | Active: %d | Cities covered: %d",
            buyers.size(),
            activeCount,
            uniqueCities.size()
        ));
    }

    private void updateMapMarkers() {
        if (mapWebView == null || mapWebView.getEngine() == null) {
            return;
        }

        String script = "window.renderMarkers(" + buildMarkersArrayLiteral() + ");";
        try {
            mapWebView.getEngine().executeScript(script);
        } catch (RuntimeException ignored) {
            // Map page may not be ready yet; initializeMap listener will retry.
        }
    }

    private void setDraftMarkerOnMap(Double latitude, Double longitude) {
        if (mapWebView == null || mapWebView.getEngine() == null) {
            return;
        }

        String latArg = latitude == null ? "null" : latitude.toString();
        String lngArg = longitude == null ? "null" : longitude.toString();

        try {
            mapWebView.getEngine().executeScript("window.setDraftMarker(" + latArg + "," + lngArg + ");");
        } catch (RuntimeException ignored) {
            // Map page may not be ready yet.
        }
    }

    private void syncCoordinatesFromWebView() {
        if (mapWebView == null || mapWebView.getEngine() == null) {
            return;
        }

        try {
            Object latObj = mapWebView.getEngine().executeScript(
                "window.__draftLocation ? window.__draftLocation.lat : null"
            );
            Object lngObj = mapWebView.getEngine().executeScript(
                "window.__draftLocation ? window.__draftLocation.lng : null"
            );

            if (latObj instanceof Number latNum && lngObj instanceof Number lngNum) {
                selectedLatitude = latNum.doubleValue();
                selectedLongitude = lngNum.doubleValue();
                updateSelectedLocationLabel();
            }
        } catch (RuntimeException ignored) {
            // WebView may be refreshing; keep existing selected coordinates if present.
        }
    }

    private void updateSelectedLocationLabel() {
        if (selectedLatitude == null || selectedLongitude == null) {
            selectedLocationLabel.setText("No point selected yet. Click on map to pin location.");
            return;
        }

        selectedLocationLabel.setText(String.format("Selected point: %.6f, %.6f", selectedLatitude, selectedLongitude));
    }

    @FXML
    private void refreshWeatherFromSelectedPoint() {
        fetchWeatherForSelectedPoint();
    }

    private void fetchWeatherForSelectedPoint() {
        syncCoordinatesFromWebView();

        if (selectedLatitude == null || selectedLongitude == null) {
            weatherHeaderLabel.setText("Weather Conditions");
            weatherMetaLabel.setText("Select a map point to fetch weather.");
            weatherTempLabel.setText("--");
            weatherWindLabel.setText("--");
            weatherRainLabel.setText("--");
            weatherComfortBar.setProgress(0);
            weatherWindRiskBar.setProgress(0);
            weatherRainRiskBar.setProgress(0);
            weatherComfortValueLabel.setText("--");
            weatherWindRiskValueLabel.setText("--");
            weatherRainRiskValueLabel.setText("--");
            weatherStatusLabel.setText("Waiting for location selection");
            weatherStatusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
            applyWeatherBarStyles(0, 0, 0);
            return;
        }

        double lat = selectedLatitude;
        double lon = selectedLongitude;

        weatherHeaderLabel.setText("Weather Conditions");
        weatherMetaLabel.setText(String.format(Locale.US, "Point: %.4f, %.4f", lat, lon));
        weatherTempLabel.setText("Loading...");
        weatherWindLabel.setText("Loading...");
        weatherRainLabel.setText("Loading...");
        weatherComfortBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        weatherWindRiskBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        weatherRainRiskBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        weatherComfortValueLabel.setText("...");
        weatherWindRiskValueLabel.setText("...");
        weatherRainRiskValueLabel.setText("...");
        weatherStatusLabel.setText("Fetching latest weather data...");
        weatherStatusLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
        applyWeatherBarStyles(0.45, 0.45, 0.45);

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return weatherService.fetchCurrentWeather(lat, lon);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            })
            .thenAccept(weatherData -> Platform.runLater(() -> applyWeatherData(weatherData, lat, lon)))
            .exceptionally(ex -> {
                Platform.runLater(() -> showWeatherError(ex));
                return null;
            });
    }

    private void applyWeatherData(WeatherData weatherData, double lat, double lon) {
        double temp = weatherData.getTemperatureCelsius();
        double wind = weatherData.getWindSpeedKmh();
        double rain = weatherData.getRainMm();

        double comfortScore = 1.0 - Math.min(Math.abs(temp - 22.0) / 22.0, 1.0);
        double windRisk = Math.min(wind / 60.0, 1.0);
        double rainRisk = Math.min(rain / 10.0, 1.0);
        String recommendation = weatherData.buildRecommendation();

        weatherHeaderLabel.setText("Weather Conditions");
        weatherMetaLabel.setText(String.format(Locale.US, "Point: %.4f, %.4f", lat, lon));
        weatherTempLabel.setText(String.format(Locale.US, "%.1f °C", temp));
        weatherWindLabel.setText(String.format(Locale.US, "%.1f km/h", wind));
        weatherRainLabel.setText(String.format(Locale.US, "%.1f mm", rain));
        weatherComfortBar.setProgress(comfortScore);
        weatherWindRiskBar.setProgress(windRisk);
        weatherRainRiskBar.setProgress(rainRisk);
        weatherComfortValueLabel.setText(String.format(Locale.US, "%d%%", (int) Math.round(comfortScore * 100)));
        weatherWindRiskValueLabel.setText(String.format(Locale.US, "%d%%", (int) Math.round(windRisk * 100)));
        weatherRainRiskValueLabel.setText(String.format(Locale.US, "%d%%", (int) Math.round(rainRisk * 100)));
        applyWeatherBarStyles(comfortScore, windRisk, rainRisk);
        weatherStatusLabel.setText(recommendation);

        if (recommendation.toLowerCase(Locale.ROOT).contains("bad weather")) {
            weatherStatusLabel.setStyle("-fx-text-fill: #991b1b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #fee2e2; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
        } else {
            weatherStatusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
        }
    }

    private void showWeatherError(Throwable throwable) {
        Throwable cause = throwable instanceof RuntimeException && throwable.getCause() != null
            ? throwable.getCause()
            : throwable;

        weatherHeaderLabel.setText("Weather Conditions");
        weatherMetaLabel.setText("No live data available");
        weatherTempLabel.setText("--");
        weatherWindLabel.setText("--");
        weatherRainLabel.setText("--");
        weatherComfortBar.setProgress(0);
        weatherWindRiskBar.setProgress(0);
        weatherRainRiskBar.setProgress(0);
        weatherComfortValueLabel.setText("--");
        weatherWindRiskValueLabel.setText("--");
        weatherRainRiskValueLabel.setText("--");
        weatherStatusLabel.setText("Weather API failed: " + cause.getMessage());
        weatherStatusLabel.setStyle("-fx-text-fill: #991b1b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #fee2e2; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
        applyWeatherBarStyles(0, 0, 0);
    }

    private void resetWeatherCard() {
        weatherHeaderLabel.setText("Weather Conditions");
        weatherMetaLabel.setText("Select a map point to fetch weather.");
        weatherTempLabel.setText("--");
        weatherWindLabel.setText("--");
        weatherRainLabel.setText("--");
        weatherComfortBar.setProgress(0);
        weatherWindRiskBar.setProgress(0);
        weatherRainRiskBar.setProgress(0);
        weatherComfortValueLabel.setText("--");
        weatherWindRiskValueLabel.setText("--");
        weatherRainRiskValueLabel.setText("--");
        weatherStatusLabel.setText("Click on map to load weather");
        weatherStatusLabel.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-background-radius: 9; -fx-padding: 6 10 6 10;");
        applyWeatherBarStyles(0, 0, 0);
    }

    private void applyWeatherBarStyles(double comfort, double windRisk, double rainRisk) {
        weatherComfortBar.setStyle(progressBarStyleFromScore(comfort, false));
        weatherWindRiskBar.setStyle(progressBarStyleFromScore(windRisk, true));
        weatherRainRiskBar.setStyle(progressBarStyleFromScore(rainRisk, true));
    }

    private String progressBarStyleFromScore(double score, boolean isRisk) {
        double clamped = Math.max(0.0, Math.min(1.0, score));
        String color;

        if (isRisk) {
            if (clamped <= 0.35) {
                color = "#16a34a";
            } else if (clamped <= 0.65) {
                color = "#ca8a04";
            } else {
                color = "#dc2626";
            }
        } else {
            if (clamped >= 0.65) {
                color = "#16a34a";
            } else if (clamped >= 0.35) {
                color = "#ca8a04";
            } else {
                color = "#dc2626";
            }
        }

        return String.format("-fx-accent: %s;", color);
    }

    public final class MapBridge {
        public void onMapClicked(double latitude, double longitude) {
            Platform.runLater(() -> {
                selectedLatitude = latitude;
                selectedLongitude = longitude;
                updateSelectedLocationLabel();
                formStatusLabel.setText("Location pinned on map. Complete the form and save.");
                fetchWeatherForSelectedPoint();
            });
        }
    }

    private String buildMarkersArrayLiteral() {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < buyers.size(); i++) {
            RecyclingBuyer buyer = buyers.get(i);
            if (i > 0) {
                builder.append(',');
            }

            builder.append('{')
                .append("name:").append(toJsString(buyer.getBuyerName())).append(',')
                .append("type:").append(toJsString(buyer.getRecyclingType())).append(',')
                .append("city:").append(toJsString(buyer.getCity())).append(',')
                .append("status:").append(toJsString(buyer.getStatus())).append(',')
                .append("address:").append(toJsString(buyer.getAddress())).append(',')
                .append("lat:").append(buyer.getLatitude()).append(',')
                .append("lng:").append(buyer.getLongitude())
                .append('}');
        }

        builder.append(']');
        return builder.toString();
    }

    private String toJsString(String value) {
        if (value == null) {
            return "null";
        }

        String escaped = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r");

        return "'" + escaped + "'";
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
