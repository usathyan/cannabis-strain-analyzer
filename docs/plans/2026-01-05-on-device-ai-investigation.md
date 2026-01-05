# On-Device AI Investigation

**Status:** Planning for future release
**Goal:** Privacy - run text analysis locally instead of cloud

## Summary

Replace Claude Haiku (text model) with on-device model for privacy. Keep OpenRouter for vision (menu scanning).

## Current Haiku Usage

| Use Case | Tokens | Privacy Risk | Replace? |
|----------|--------|--------------|----------|
| Terpene generation | ~256 | High - reveals strain interests | Yes |
| Menu HTML parsing | ~4K | Medium | Optional |
| Vision cleanup | ~2K | Low | Optional |

## Recommended Approach

**Phase 1:** Replace terpene generation only (smallest scope, highest privacy impact)

**Model Candidates:**
- FunctionGemma (270M) - Google, function calling, 50 tok/s on mobile
- Gemma 3n (2B) - Google, multimodal, but overkill for text-only
- Phi-3-mini (3.8B) - Microsoft, good reasoning

## Technical Requirements

- Generate JSON with 8 terpene values (0.0-1.0)
- Domain knowledge of cannabis strains
- Works on iOS (Core ML / LiteRT) and Android (LiteRT / llama.cpp)
- App size impact: ~300MB-1GB depending on model

## Resources

- [Google AI Edge](https://developers.googleblog.com/google-ai-edge-small-language-models-multimodality-rag-function-calling/)
- [Unsloth FunctionGemma](https://unsloth.ai/docs/models/functiongemma)
- [Awesome Mobile LLM](https://github.com/stevelaskaridis/awesome-mobile-llm)

## Next Steps

1. Prototype terpene generation with FunctionGemma
2. Fine-tune on cannabis strain data if needed
3. Integrate with Kotlin Multiplatform (expect/actual for iOS/Android)
4. A/B test accuracy vs Claude Haiku
