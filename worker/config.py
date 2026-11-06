"""
Configuration for the BioCLIP-2 observation worker.

All configuration is loaded from environment variables with sensible defaults
that match the Spring Boot backend's RabbitMQ and S3/RustFS setup.
"""

import os
import torch

def _get_env(name: str, default: str) -> str:
    """Get an environment variable with a fallback default."""
    return os.environ.get(name, default)


def _get_env_int(name: str, default: int) -> int:
    """Get an integer environment variable with a fallback default."""
    try:
        return int(os.environ.get(name, default))
    except ValueError:
        return default


def _get_env_float(name: str, default: float) -> float:
    """Get a float environment variable with a fallback default."""
    try:
        return float(os.environ.get(name, default))
    except ValueError:
        return default


class Config:
    """Worker configuration loaded from environment variables."""

    # --- RabbitMQ ---
    RABBITMQ_HOST: str = _get_env("RABBITMQ_HOST", "localhost")
    RABBITMQ_PORT: int = _get_env_int("RABBITMQ_PORT", 5672)
    RABBITMQ_USER: str = _get_env("RABBITMQ_USER", "rabbitmq")
    RABBITMQ_PASSWORD: str = _get_env("RABBITMQ_PASSWORD", "rabbitmq")
    RABBITMQ_EXCHANGE: str = _get_env("RABBITMQ_EXCHANGE", "observation.jobs.exchange")
    RABBITMQ_INPUT_QUEUE: str = _get_env("RABBITMQ_INPUT_QUEUE", "observation.jobs")
    RABBITMQ_RESPONSE_QUEUE: str = _get_env("RABBITMQ_RESPONSE_QUEUE", "observation.jobs.response")
    RABBITMQ_RESPONSE_ROUTING_KEY: str = _get_env("RABBITMQ_RESPONSE_ROUTING_KEY", "observation.jobs.response")

    # --- S3 / RustFS ---
    S3_ENDPOINT: str = _get_env("S3_ENDPOINT", "http://localhost:9000")
    S3_BUCKET: str = _get_env("S3_BUCKET", "biojaveriana")
    S3_ACCESS_KEY: str = _get_env("S3_ACCESS_KEY", "rustfsadmin")
    S3_SECRET_KEY: str = _get_env("S3_SECRET_KEY", "rustfsadmin")
    S3_REGION: str = _get_env("S3_REGION", "us-east-1")
    S3_FORCE_PATH_STYLE: bool = _get_env("S3_FORCE_PATH_STYLE", "true").lower() in ("true", "1", "yes")

    # --- Model ---
    MODEL_NAME: str = _get_env("MODEL_NAME", "imageomics/bioclip-2")
    DEVICE: str = _get_env("DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
    MODEL_PRECISION: str = _get_env("MODEL_PRECISION", "float16" if torch.cuda.is_available() else "float32")

    # --- Inference ---
    KINGDOM_LABELS: list[str] = ["Animalia", "Plantae", "Fungi"]
    
    # ===== Bogotá, Colombia Species (elevation ~2,640m, Andes region) =====
    # Data sourced from GBIF and iNaturalist observations for Bogotá, Colombia
    DEFAULT_ANIMAL_SPECIES: list[str] = [
        # --- BIRDS (Bogotá has 300+ bird species; most commonly observed) ---
        # Urban/Widespread birds
        "Columba livia",           # Rock Pigeon - extremely common in Bogotá
        "Passer domesticus",       # House Sparrow - very common in cities
        "Troglodytes aedon",       # Wren - very common in Bogotá
        "Troglodytes musculus",    # House Wren - very common
        "Myiozetetes similis",     # Social Flycatcher - common
        "Pitangus sulphuratus",    # Great Kiskadee - very common
        "Pitangus luteolus",       # Lesser Kiskadee - common
        "Coereba flaveola",       # Bananaquit - very common
        "Leptotila jamaicensis",  # White-tipped Dove - very common
        "Patagioenas leucocephala", # White-headed Pigeon - common
        "Turdus amaurochalinus",  # Dark-sided Thrush - common
        "Turdus chrysops",        # Golden-faced Thrush - present
        "Myrmotherula campana",   # Amazonian Manakin - present
        
        # Raptors/Vultures - very visible in Bogotá skies
        "Cathartes aura",          # Turkey Vulture - very common
        "Coragyps atratus",        # Black Vulture - very common
        "Caracara plancus",        # Crested Caracara - common
        "Milvago chimachoma",      # Yellow-headed Caracara - common
        "Buteo jamaicensis",       # Red-tailed Hawk - common
        "Buteo albonotatus",       # Yellow-thighed Hawk - present
        "Falco femoralis",         # Aplomado Falcon - present
        
        # Other common birds
        "Geotrygon montana",      # Ruddy Quail-Dove - present
        "Euphonia gouldi",        # Gould's Euphonia - present
        "Chlorophanes spiza",     # Green Honeycreeper - present
        "Ramphastos brevis",      # Chestnut-mandibled Toucan - occasional
        "Ramphastos dicolorus",   # Red-breasted Toucan - occasional
        "Picumnus cinnamomeus",   # Cinnamon Woodpecker - present
        "Melanerpes rubricapillus", # Red-crowned Woodpecker - present
        "Diglossa albilatera",    # White-sided Flowerpiercer - high elevation
        "Diglossa plumbea",       # Plumbeous Flowerpiercer - high elevation
        "Icterus chrysater",      # Yellow-throated Oriole - seasonal
        "Gracila pallidiceps",    # Pallid-headed Tyrannulet - present
        "Contopus bogotensis",    # Bogotá Tyrannulet - endemic
        "Conopias trivirgatus",   # Three-striped Flycatcher - present
        "Corvus corax",           # Common Raven - occasional
        "Strix aluco",            # Tawny Owl - present
        "Tyto alba",              # Barn Owl - present
        "Ardea alba",             # Great Egret - wetlands
        "Ardea cinerea",          # Grey Heron - wetlands
        
        # --- MAMMALS ---
        "Nasua nasua",            # White-nosed Coati - very common
        "Tamandua tetradactyla",  # Southern Tamandua - common
        "Didelphis marsupialis",  # Common Opossum - common
        "Procyon lotor",          # Raccoon - common
        "Cuniculus paca",         # Lowland Paca - present
        "Myrmecophaga tridactyla", # Giant Anteater - present
        "Saimiri sciureus",       # Common Squirrel Monkey - present
        "Alouatta seniculus",     # Red Howler Monkey - present
        "Mephitis mephitis",      # Striped Skunk - present
        "Conepatus chinga",       # Chinga - present
        "Leopardus pardalis",     # Jaguarundi - rare
        "Leopardus geoffroyi",    # Geoffroy's Cat - rare
        "Microtus bogotensis",    # Bogotá Grass Mouse - endemic
        "Akodon bogotensis",      # Bogotá Grass Mouse - endemic
        "Rhipidomys macrurus",    # Long-tailed Rice Rat - present
        "Phyllostomus discolor",  # Big-eared Brown Bat - common
        "Desmodus rotundus",      # Vampire Bat - present
        "Tadarida brasiliensis",  # Brazilian Free-tailed Bat - present
        "Eptesicus brasiliensis", # Brazilian Big-eared Bat - present
        
        # --- REPTILES ---
        "Anolis fuscoauratus",    # Brown Anole - very common
        "Phyllodactylus bogotensis", # Bogotá Leaf-toed Gecko - endemic
        "Tropidurus hispidus",    # Spinytail Iguana - present
        "Ameivula ocellifera",    # Ocellated Whiptail - present
        
        # --- AMPHIBIANS ---
        "Dendropsophus ebraccatus", # Hyla Frog - very common
        "Epipedobates tricolor",  # Three-colored Poison Dart Frog - present
        "Hyloxalus azureus",      # Azure Treefrog - present
        "Allobates femoralis",    # Bicolored Running Frog - common
        "Leptodactylus pentadactylus", # Five-fingered Whistling Frog - common
        "Dendropsophus minutus", # Lesser Treefrog - common
        "Oophaga pumilio",        # Strawberry Poison Dart Frog - present
        "Atelopus zeteki",        # Panamanian Golden Frog - present
        "Rhinella marina",        # Cane Toad - present
        "Rhaebo haematogaster",   # Red-spotted Toad - present
        "Phylomedusa bicolor",    # Monkey Frog - present
        "Scinax nasicus",         # Treefrog - common
        "Leptodactylus fragilis", # Frosted Whistling Frog - present
        
        # --- INSECTS ---
        "Apis mellifera",         # Western Honey Bee - very common
        "Danaus plexippus",       # Monarch Butterfly - migratory
        "Heliconius melpomene",   # Heliconius Butterfly - common
        "Morpho menelaus",        # Menelaus Blue Morpho - common
        "Parides ascanius",       # Giant Swallowtail - present
        "Agraulis vanillae",      # Greta Oto - common
        "Caligo memnon",          # Owl Butterfly - present
        "Papilio demoleus",       # Lemon Butterfly - present
        "Papilio polytes",        # Common Mormon - present
        "Bombus impatiens",       # Common Bumble Bee - present
        "Vespa crabro",           # European Giant Hornet - present
        "Formica rufa",           # Wood Ant - present
        "Lucanus cervus",         # Stag Beetle - present
    ]
    ANIMAL_SPECIES_LIST: list[str] = []
    
    # ===== PLANT SPECIES for Bogotá, Colombia =====
    # Data sourced from GBIF and iNaturalist observations for Bogotá, Colombia
    DEFAULT_PLANT_SPECIES: list[str] = [
        "Elaphoglossum minutum",
        "Ulex europaeus",
        "Cenchrus clandestinus",
        "Solanum oblongifolium",
        "Acacia decurrens",
        "Eucalyptus globulus",
        "Genista monspessulana",
        "Miconia squamulosa",
        "Juglans neotropica",
        "Verbesina crassiramea",
        "Piper bogotense",
        "Baccharis latifolia",
        "Solanum nigrum",
        "Smallanthus pyramidalis",
        "Alnus acuminata",
        "Cupressus lusitanica",
        "Pittosporum undulatum",
        "Trifolium repens",
        "Rubus bogotensis",
        "Trifolium pratense",
        "Acacia melanoxylon",
        "Fuchsia boliviana",
        "Digitalis purpurea",
        "Espeletia corymbosa",
        "Sambucus nigra",
        "Quercus humboldtii",
        "Vallea stipularis",
        "Bocconia frutescens",
        "Holcus lanatus",
        "Ricinus communis",
        "Rhynchospora macrochaeta",
        "Thunbergia alata",
        "Lafoensia acuminata",
        "Myrcianthes leucoxyla",
        "Duranta mutisii",
        "Viburnum triphyllum",
        "Taraxacum officinale",
        "Physalis peruviana",
        "Tecoma stans",
        "Ficus americana",
        "Prunus serotina",
        "Abatia parviflora",
        "Cucurbita ficifolia",
        "Persicaria capitata",
        "Espeletia grandiflora",
        "Feijoa sellowiana",
        "Escallonia pendula",
        "Xylosma spiculifera",
        "Frangula goudotiana",
        "Liquidambar styraciflua",
        "Sonchus oleraceus",
        "Morella parvifolia",
        "Salix humboldtiana",
        "Impatiens sodenii",
        "Schinus areira",
        "Achyrocline bogotensis",
        "Monochaetum myrtoideum",
        "Senecio madagascariensis",
        "Oreopanax incisus",
        "Macleania rupestris",
        "Morella pubescens",
        "Vinca major",
        "Puya nitida",
        "Citharexylum reticulatum",
        "Dodonaea viscosa",
        "Citharexylum subflavescens",
        "Solanum ovalifolium",
        "Passiflora tripartita",
        "Weinmannia tomentosa",
        "Rumex acetosella",
        "Arcytophyllum nitidum",
        "Persicaria nepalensis",
        "Solanum americanum",
        "Streptosolen jamesonii",
        "Paepalanthus alpinus",
        "Hydrocotyle bonplandii",
        "Oxalis corniculata",
        "Cotula australis",
        "Bidens laevis",
        "Hypochaeris radicata",
        "Bejaria resinosa",
        "Poa annua",
        "Otholobium mexicanum",
        "Oxalis filiformis",
        "Hydrocotyle ranunculoides",
        "Senna viarum",
        "Oreopanax bogotensis",
        "Linochilus phylicoides",
        "Hesperomeles goudotiana",
        "Schoenoplectus californicus",
        "Myrsine guianensis",
        "Bucquetia glutinosa",
        "Cedrela montana",
        "Orthrosanthus chimboracensis",
        "Cavendishia bracteata",
        "Hydrocotyle andina",
        "Cirsium vulgare",
        "Carludovica palmata",
        "Polygonum punctatum",
        "Rumex obtusifolius",
        "Fraxinus uhdei",
        "Phytolacca bogotensis",
        "Anthoxanthum odoratum",
        "Syzygium paniculatum",
        "Erigeron bonariensis",
        "Fraxinus chinensis",
        "Paramochloa effusa",
        "Euphorbia peplus",
        "Gaultheria myrsinoides",
        "Croton hibiscifolius",
        "Ficus soatensis",
        "Stellaria media",
        "Brugmansia sanguinea",
        "Gaiadendron punctatum",
        "Ageratina asclepiadea",
        "Brugmansia arborea",
        "Schinus molle",
        "Galium hypocarpium",
        "Spergula arvensis",
        "Escallonia paniculata",
        "Sisyrinchium micranthum",
        "Pleopeltis macrocarpa",
        "Vasconcellea pubescens",
        "Pinus patula",
        "Syzygium myrtifolium",
        "Renealmia thyrsoidea",
        "Aragoa abietina",
        "Baccharis macrantha",
        "Nertera granadensis",
        "Conium maculatum",
        "Croton coriaceus",
        "Kalanchoe densiflora",
        "Myrsine coriacea",
        "Veronica serpyllifolia",
        "Vaccinium floribundum",
        "Typha latifolia",
        "Muehlenbeckia tamnifolia",
        "Tropaeolum majus",
        "Senecio vulgaris",
        "Cestrum mutisii",
        "Bomarea multiflora",
        "Gaultheria anastomosans",
        "Palicourea boqueronensis",
        "Brachyotum strigosum",
        "Cyperus eragrostis",
        "Lycopodium clavatum",
        "Eucalyptus camaldulensis",
        "Solanum nigrescens",
        "Cymbalaria muralis",
        "Azolla filiculoides",
        "Phyllanthus salviifolius",
        "Retrophyllum rospigliosii",
        "Paraserianthes lophantha",
        "Clusia multiflora",
        "Miconia ligustrina",
        "Cestrum buxifolium",
        "Verbena litoralis",
        "Masdevallia ignea",
        "Baccharis bogotensis",
        "Siphocampylus columnae",
        "Gamochaeta americana",
        "Clethra fimbriata",
        "Rubus glaucus",
        "Varronia cylindristachya",
        "Cotoneaster pannosus",
        "Pontederia crassipes",
        "Linochilus rosmarinifolius",
        "Passiflora bogotensis",
        "Hypericum juniperinum",
        "Capsella bursa-pastoris",
        "Rumex crispus",
        "Sphagnum magellanicum",
        "Castilleja fissifolia",
        "Lepidium bipinnatifidum",
        "Oldenlandia corymbosa",
        "Cobaea scandens",
        "Dahlia imperialis",
        "Hesperomeles obtusifolia",
        "Solanum quitoense",
        "Callianthe picta",
        "Senecio carbonellii",
        "Passiflora tarminiana",
        "Dryopteris wallichiana",
        "Potentilla indica",
        "Puya goudotiana",
        "Senna multiglandulosa",
        "Solanum marginatum",
        "Pleroma urvilleanum",
        "Acacia baileyana",
        "Rubus floribundus",
        "Gaylussacia buxifolia",
        "Monticalia pulchella",
        "Monnina aestuans",
        "Veronica persica",
        "Juncus effusus",
        "Meriania nobilis",
        "Drimys granadensis",
        "Cucurbita pepo",
        "Geranium santanderiense",
        "Castilleja arvensis",
        "Castratella piloselloides",
        "Lycianthes lycioides",
        "Tradescantia fluminensis",
        "Ilex kunthiana",
        "Viburnum tinoides",
        "Monnina salicifolia",
        "Erythrina rubrinervia",
        "Pteridium esculentum",
        "Rumex conglomeratus",
        "Plantago major",
        "Fuchsia paniculata",
        "Callianthe megapotamica",
        "Ageratina tinifolia",
        "Hypericum strictum",
        "Parablechnum loxense",
        "Chusquea tessellata",
        "Solanum pseudocapsicum",
        "Nicotiana tabacum",
        "Varronia cylindrostachya",
        "Salvia palifolia",
        "Magnolia grandiflora",
        "Crocosmia crocosmiiflora",
        "Lolium multiflorum",
        "Calliandra trinervia",
        "Croton mutisianus",
        "Chaetogastra grossa",
        "Espeletia argentea",
        "Alonsoa meridionalis",
        "Carex bonplandii",
        "Salpichroa tristis",
        "Ficus tequendamae",
        "Billia rosea",
        "Ludwigia peruviana",
        "Gentianella corymbosa",
        "Pteridium aquilinum",
        "Chlorophytum comosum",
        "Ligustrum lucidum",
        "Acacia dealbata",
        "Pseudognaphalium cheiranthifolium",
        "Dendrophorbium americanum",
        "Palicourea paniculata",
        "Brugmansia candida",
        "Galinsoga quadriradiata",
        "Yucca gigantea",
        "Sagina procumbens",
        "Phoenix roebelenii",
        "Critoniopsis bogotana",
        "Monticalia vaccinioides",
        "Salvia palaefolia",
        "Lemna gibba",
        "Lobularia maritima",
        "Ludwigia peploides",
        "Pleurozium schreberi",
        "Tillandsia biflora",
        "Verbesina arborea",
        "Chusquea scandens",
        "Epidendrum zipaquiranum",
        "Piper barbatum",
        "Lantana camara",
        "Andesanthus lepidotus",
        "Medicago lupulina",
        "Bidens pilosa",
        "Ipomoea purpurea",
        "Nasturtium officinale",
        "Symplocos theiformis",
        "Solanum stellatiglandulosum",
        "Epidendrum arachnoglossum",
        "Oxalis latifolia",
        "Gaultheria erecta",
        "Duranta erecta",
        "Passiflora mixta",
        "Hydrocharis laevigata",
        "Noticastrum marginatum",
        "Lourteigia stoechadifolia",
        "Hypochaeris sessiliflora",
        "Miconia elaeoides",
        "Diphasiastrum thyoides",
        "Halenia asclepiadea",
        "Hypericum mexicanum",
        "Hypericum goyanesii",
        "Excremis coarctata",
        "Solanum betaceum",
        "Monticalia abietina",
        "Chaetolepis microphylla",
        "Solanum caripense",
        "Helichrysum luteoalbum",
        "Cyrtochilum revolutum",
        "Erigeron karvinskianus",
        "Helicodontium capillare",
        "Sonchus asper",
        "Cecropia telenitida",
        "Acaena cylindristachya",
        "Lemna obscura",
        "Arcytophyllum muticum",
        "Tara spinosa",
        "Stenorrhynchos albidomaculatum",
        "Smilax tomentosa",
        "Salvia leucantha",
        "Neobartsia santolinifolia",
        "Myrcianthes rhopaloides",
        "Solanum sessiliflorum",
    ]
    PLANT_SPECIES_LIST: list[str] = []
    TEMPERATURE: float = _get_env_float("INFERENCE_TEMPERATURE", 100.0)

    # --- Concurrency ---
    CONSUMER_COUNT: int = _get_env_int("CONSUMER_COUNT", 4)

    # --- Misc ---
    LOG_LEVEL: str = _get_env("LOG_LEVEL", "INFO")
    SHUTDOWN_TIMEOUT: float = _get_env_float("SHUTDOWN_TIMEOUT", 30.0)

    def __post_init__(self) -> None:
        """Load species list from environment or use defaults."""
        species_env = _get_env("ANIMAL_SPECIES_LIST", "")
        if species_env:
            self.ANIMAL_SPECIES_LIST = [s.strip() for s in species_env.split(",") if s.strip()]
        else:
            self.ANIMAL_SPECIES_LIST = list(self.DEFAULT_ANIMAL_SPECIES)
        
        plant_env = _get_env("PLANT_SPECIES_LIST", "")
        if plant_env:
            self.PLANT_SPECIES_LIST = [s.strip() for s in plant_env.split(",") if s.strip()]
        else:
            self.PLANT_SPECIES_LIST = list(self.DEFAULT_PLANT_SPECIES)

    def get_animal_labels(self) -> list[str]:
        """Get the formatted labels for animal species classification."""
        species_list = self.ANIMAL_SPECIES_LIST if self.ANIMAL_SPECIES_LIST else self.DEFAULT_ANIMAL_SPECIES
        return [f"a photo of {species}" for species in species_list]

    def get_plant_labels(self) -> list[str]:
        """Get the formatted labels for plant species classification."""
        species_list = self.PLANT_SPECIES_LIST if self.PLANT_SPECIES_LIST else self.DEFAULT_PLANT_SPECIES
        return [f"a photo of {species}" for species in species_list]

    def get_kingdom_labels(self) -> list[str]:
        """Get the formatted labels for kingdom classification."""
        return [f"a photo of {label}" for label in self.KINGDOM_LABELS]


# Singleton config instance
config = Config()
