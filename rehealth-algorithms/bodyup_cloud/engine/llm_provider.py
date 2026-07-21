"""
LLM multi-provider abstraction (Claude / OpenAI / OpenRouter).

Part of bodyup_cloud.engine — V1 specification.
"""

from abc import ABC, abstractmethod


class LLMProvider(ABC):
    """Base class for all LLM providers."""

    @abstractmethod
    def generate(self, system_prompt: str, user_prompt: str) -> str:
        ...


class ClaudeProvider(LLMProvider):
    """Anthropic Claude via the official SDK."""

    def __init__(self, api_key: str, model: str = "claude-sonnet-4-20250514"):
        import anthropic

        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = model

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.messages.create(
            model=self.model,
            max_tokens=1024,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        return response.content[0].text


class OpenAIProvider(LLMProvider):
    """OpenAI GPT models via the official SDK."""

    def __init__(self, api_key: str, model: str = "gpt-4o"):
        from openai import OpenAI

        self.client = OpenAI(api_key=api_key)
        self.model = model

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            max_tokens=1024,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        )
        return response.choices[0].message.content


class OpenRouterProvider(LLMProvider):
    """OpenRouter proxy (OpenAI-compatible API)."""

    def __init__(self, api_key: str, model: str = "anthropic/claude-sonnet-4"):
        from openai import OpenAI

        self.client = OpenAI(
            api_key=api_key,
            base_url="https://openrouter.ai/api/v1",
        )
        self.model = model

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            max_tokens=1024,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        )
        return response.choices[0].message.content


class MiMoProvider(LLMProvider):
    """Xiaomi MiMo via Anthropic Messages API with custom base_url."""

    def __init__(
        self,
        api_key: str,
        model: str = "mimo-v2.5",
        base_url: str = "https://token-plan-cn.xiaomimimo.com/anthropic",
    ):
        import anthropic

        self.client = anthropic.Anthropic(api_key=api_key, base_url=base_url)
        self.model = model

    def generate(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.messages.create(
            model=self.model,
            max_tokens=2048,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        return response.content[0].text


def create_provider(
    provider_name: str, api_key: str, model: str | None = None, **kwargs
) -> LLMProvider:
    """Factory function to instantiate an LLM provider by name.

    Parameters
    ----------
    provider_name : str
        One of "claude", "openai", "openrouter", "mimo".
    api_key : str
        API key for the chosen provider.
    model : str or None
        Override the default model for the provider.
    """
    providers = {
        "claude": ClaudeProvider,
        "openai": OpenAIProvider,
        "openrouter": OpenRouterProvider,
        "mimo": MiMoProvider,
    }
    cls = providers.get(provider_name)
    if not cls:
        raise ValueError(f"Unknown provider: {provider_name}")
    init_kwargs: dict = {"api_key": api_key}
    if model:
        init_kwargs["model"] = model
    if provider_name == "mimo" and "base_url" in kwargs:
        init_kwargs["base_url"] = kwargs["base_url"]
    return cls(**init_kwargs)
