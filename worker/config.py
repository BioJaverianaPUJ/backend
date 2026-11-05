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
    DEFAULT_ANIMAL_SPECIES: list[str] = [
        # Mammals
        "Lynx rufus", "Ursus arctos", "Canis lupus", "Felis catus", "Panthera leo",
        "Panthera tigris", "Equus ferus caballus", "Bos taurus", "Ovis aries",
        "Capra hircus", "Sus scrofa", "Giraffa camelopardalis", "Loxodonta africana",
        "Elephas maximus", "Gorilla gorilla", "Pan troglodytes", "Homo sapiens",
        "Vulpes vulpes", "Lupinus polyphyllus", "Odocoileus virginianus",
        "Cervus elaphus", "Rangifer tarandus", "Mustela putorius", "Meles meles",
        "Martes martes", "Mustela nivalis", "Lutra lutra", "Viburnum tinus",
        # Birds
        "Corvus corax", "Corvus corone", "Buteo buteo", "Accipiter gentilis",
        "Strix aluco", "Tyto alba", "Falco peregrinus", "Aquila chrysaetos",
        "Gyps fulvus", "Pandion haliaetus", "Ciconia ciconia", "Ardea cinerea",
        "Phalacrocorax carbo", "Pelecanus onocrotalus", "Fulica atra", "Gallus gallus",
        "Anas platyrhynchos", "Cygnus olor", "Branta leucopsis", "Somateria mollissima",
        "Larus argentatus", "Sterna hirundo", "Alcedo atthis", "Dendrocopos major",
        "Parus major", "Sylvia atricapilla", "Erithacus rubecula", "Turdus merula",
        "Turdus philomelos", "Prunella modularis", "Fringilla coelebs", "Carduelis carduelis",
        "Serinus serinus", "Emberiza citrinella", "Motacilla alba", "Cuculus canorus",
        "Strix aluco", "Aegolius funereus", "Caprimulgus europaeus",
        # Reptiles & Amphibians
        "Vipera berus", "Natrix natrix", "Lacerta agilis", "Tarentola mauritanica",
        "Homo sapiens", "Bufo bufo", "Rana temporaria", "Lissotriton vulgaris",
        "Testudo hermanni", "Testudo graeca",
        # Fish
        "Salmo salar", "Oncorhynchus mykiss", "Salvelinus alpinus", "Coregonus lavaretus",
        "Esox lucius", "Perca fluviatilis", "Sander lucioperca", "Silurus glanis",
        "Acipenser sturio", "Acipenser nudiventris",
        # Insects
        "Apis mellifera", "Bombus terrestris", "Papilio machaon", "Vanessa atalanta",
        "Danaus plexippus", "Aglais urticae", "Morpho menelaus", "Lucanus cervus",
        "Cerambyx cerdo", "Rosalia alpina", "Lucanus cervus",
        # Marine
        "Delphinus delphis", "Tursiops truncatus", "Phocoena phocoena",
        "Phoca vitulina", "Monachus monachus", "Caretta caretta",
        "Lobarus edwardsii", "Pinnotheres pisum",
    ]
    ANIMAL_SPECIES_LIST: list[str] = []
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

    def get_animal_labels(self) -> list[str]:
        """Get the formatted labels for animal species classification."""
        return [f"a photo of {species}" for species in self.ANIMAL_SPECIES_LIST]

    def get_kingdom_labels(self) -> list[str]:
        """Get the formatted labels for kingdom classification."""
        return [f"a photo of {label}" for label in self.KINGDOM_LABELS]


# Singleton config instance
config = Config()
