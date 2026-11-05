## UI update

The main screen includes Live, 2D history, 3D latest/history, Lucky (random two-digit suggestions), and Settings tabs. Live and 2D history show 9:30 AM and 2:00 PM slots when the API returns `result_930` (or `result_0930` / `result_9_30`) and `result_200` (or `result_1400` / `result_2`). If those fields are not present in the API response, the app displays `--`; it does not invent result data. Lucky numbers are randomly generated suggestions, not predictions or guaranteed results.
