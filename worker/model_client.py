"""BioCLIP-2 model client for two-stage biological classification."""

import logging
from dataclasses import dataclass
from typing import Optional

import numpy as np
import open_clip
import torch
from PIL import Image
from worker.config import config

logger = logging.getLogger(__name__)


@dataclass
class ClassificationResult:
    is_animal: bool
    kingdom: str
    species: str
    certainty: float
    all_scores: list[tuple[str, float]]


class BioClipModelClient:
    def __init__(self) -> None:
        self.model: Optional[torch.nn.Module] = None
        self.preprocess: Optional[callable] = None
        self.tokenizer: Optional[callable] = None
        self._kingdom_embeddings: Optional[torch.Tensor] = None
        self._animal_embeddings: Optional[torch.Tensor] = None
        self._device: str = config.DEVICE
        self._temperature: float = config.TEMPERATURE

    def load_model(self) -> None:
        logger.info("Loading BioCLIP-2 model on device %s (%s precision)...", self._device, config.MODEL_PRECISION)
        self.model, self.train_processors, self.preprocess = open_clip.create_model_and_transforms(model_name=config.MODEL_NAME, pretrained="hf-hub")
        self.tokenizer = open_clip.get_tokenizer(config.MODEL_NAME)
        self.model.eval()
        if self._device == "cuda":
            self.model = self.model.to(self._device)
            if config.MODEL_PRECISION == "float16":
                self.model = self.model.half()
        else:
            logger.warning("Running on CPU - this will be slow for production use.")
        self._compute_kingdom_embeddings()
        logger.info("BioCLIP-2 model loaded successfully.")

    def _compute_kingdom_embeddings(self) -> None:
        labels = config.get_kingdom_labels()
        logger.info("Computing kingdom label embeddings (%d labels)...", len(labels))
        text_inputs = self.tokenizer(labels).to(self._device)
        with torch.no_grad():
            text_embeddings = self.model.encode_text(text_inputs)
            text_embeddings = text_embeddings / text_embeddings.norm(dim=-1, keepdim=True)
            self._kingdom_embeddings = text_embeddings

    def _compute_animal_embeddings(self) -> None:
        labels = config.get_animal_labels()
        logger.info("Computing animal species label embeddings (%d labels)...", len(labels))
        text_inputs = self.tokenizer(labels).to(self._device)
        with torch.no_grad():
            text_embeddings = self.model.encode_text(text_inputs)
            text_embeddings = text_embeddings / text_embeddings.norm(dim=-1, keepdim=True)
            self._animal_embeddings = text_embeddings

    def _apply_softmax(self, logits: torch.Tensor, temperature: float) -> np.ndarray:
        logits = logits / temperature
        exp_logits = torch.exp(logits - logits.max())
        return exp_logits / exp_logits.sum()

    def classify(self, image: Image.Image) -> ClassificationResult:
        kingdom_result = self._classify_kingdom(image)
        if kingdom_result["kingdom"] == "Animalia":
            species_result = self._classify_species(image)
            return ClassificationResult(is_animal=True, kingdom=kingdom_result["kingdom"], species=species_result["species"], certainty=species_result["certainty"], all_scores=[(l, s) for l, s in species_result["all_scores"][:10]])
        else:
            return ClassificationResult(is_animal=False, kingdom=kingdom_result["kingdom"], species=kingdom_result["best_label"], certainty=kingdom_result["best_score"], all_scores=list(kingdom_result["all_scores"]))

    def _classify_kingdom(self, image: Image.Image) -> dict:
        image_tensor = self.preprocess(image).unsqueeze(0).to(self._device)
        if config.MODEL_PRECISION == "float16" and self._device == "cuda":
            image_tensor = image_tensor.half()
        with torch.no_grad():
            image_embedding = self.model.encode_image(image_tensor)
            image_embedding = image_embedding / image_embedding.norm(dim=-1, keepdim=True)
            logits = torch.matmul(image_embedding, self._kingdom_embeddings.T).squeeze(0)
        probs = self._apply_softmax(logits, self._temperature)
        labels = config.get_kingdom_labels()
        kingdom_names = [label.replace("a photo of ", "") for label in labels]
        top_idx = int(np.argmax(probs))
        top_score = float(probs[top_idx])
        all_scores = list(zip(kingdom_names, probs.tolist()))
        logger.info("Kingdom classification: %s (%.4f)", kingdom_names[top_idx], top_score)
        return {"kingdom": kingdom_names[top_idx], "best_label": labels[top_idx], "best_score": top_score, "all_scores": all_scores}

    def _classify_species(self, image: Image.Image) -> dict:
        if self._animal_embeddings is None:
            self._compute_animal_embeddings()
        image_tensor = self.preprocess(image).unsqueeze(0).to(self._device)
        if config.MODEL_PRECISION == "float16" and self._device == "cuda":
            image_tensor = image_tensor.half()
        with torch.no_grad():
            image_embedding = self.model.encode_image(image_tensor)
            image_embedding = image_embedding / image_embedding.norm(dim=-1, keepdim=True)
            logits = torch.matmul(image_embedding, self._animal_embeddings.T).squeeze(0)
        probs = self._apply_softmax(logits, self._temperature)
        labels = config.get_animal_labels()
        species_names = [label.replace("a photo of ", "") for label in labels]
        top_idx = int(np.argmax(probs))
        top_score = float(probs[top_idx])
        sorted_indices = np.argsort(probs)[::-1][:10]
        all_scores = [(species_names[i], float(probs[i])) for i in sorted_indices]
        logger.info("Species classification (animal): %s (%.4f)", species_names[top_idx], top_score)
        return {"species": species_names[top_idx], "certainty": top_score, "all_scores": all_scores}


_model_client: Optional[BioClipModelClient] = None


def get_model_client() -> BioClipModelClient:
    global _model_client
    if _model_client is None:
        _model_client = BioClipModelClient()
    return _model_client


def load_model() -> BioClipModelClient:
    client = get_model_client()
    client.load_model()
    return client