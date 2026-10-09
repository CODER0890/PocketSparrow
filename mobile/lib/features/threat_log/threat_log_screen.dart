import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../core/local_db.dart';
import '../../core/threat_model.dart';
import '../../ui/theme/app_colors.dart';
import '../../ui/theme/app_radius.dart';
import '../../ui/theme/app_spacing.dart';
import '../../ui/theme/app_text_styles.dart';
import '../../ui/widgets/app_card.dart';
import '../../ui/widgets/app_scaffold.dart';
import '../../ui/widgets/empty_state.dart';
import '../../ui/widgets/input_field.dart';
import '../../ui/widgets/threat_list_tile.dart';

class ThreatLogScreen extends StatefulWidget {
  const ThreatLogScreen({super.key});

  @override
  State<ThreatLogScreen> createState() => _ThreatLogScreenState();
}

class _ThreatLogScreenState extends State<ThreatLogScreen> {
  String _selectedFilter = 'All'; // All, Blocked, Caution, Safe
  String _searchQuery = '';
  final TextEditingController _searchController = TextEditingController();
  List<ThreatResult> _allThreats = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadThreats();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadThreats() async {
    setState(() => _isLoading = true);
    final list = await LocalDb().getAllThreats();
    if (mounted) {
      setState(() {
        _allThreats = list;
        _isLoading = false;
      });
    }
  }

  List<ThreatResult> get _filteredThreats {
    return _allThreats.where((t) {
      if (_selectedFilter == 'Blocked' && !t.isBlocked) return false;
      if (_selectedFilter == 'Caution' && !t.isCaution) return false;
      if (_selectedFilter == 'Safe' && !t.isSafe) return false;

      if (_searchQuery.isNotEmpty) {
        final query = _searchQuery.toLowerCase();
        final matchesInput = t.rawInput.toLowerCase().contains(query);
        final matchesReason = t.reasons.any((r) => r.toLowerCase().contains(query));
        return matchesInput || matchesReason;
      }
      return true;
    }).toList();
  }

  void _confirmClearAll() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(borderRadius: AppRadius.card),
        title: Row(
          children: [
            const Icon(LucideIcons.trash, color: AppColors.blocked, size: 20),
            const SizedBox(width: AppSpacing.sm),
            Text('Clear All Logs?', style: AppTextStyles.headingMedium),
          ],
        ),
        content: Text(
          'This will permanently wipe all local threat audit records from encrypted on-device storage.',
          style: AppTextStyles.bodyMedium,
        ),
        actions: [
          TextButton(
            child: Text('Cancel', style: AppTextStyles.labelLarge.copyWith(color: AppColors.textMuted)),
            onPressed: () => Navigator.pop(ctx),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.blocked,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: AppRadius.button),
            ),
            child: const Text('Clear Database'),
            onPressed: () async {
              Navigator.pop(ctx);
              final state = context.read<AppStateProvider>();
              await state.clearAllLogs();
              await _loadThreats();
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Audit log cleared successfully')),
                );
              }
            },
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredThreats;

    return AppScaffold(
      title: Text(
        'Threat Audit Log',
        style: AppTextStyles.headingLarge,
      ),
      actions: [
        IconButton(
          icon: const Icon(LucideIcons.trash, color: AppColors.textSecondary, size: 18),
          tooltip: 'Clear All Logs',
          onPressed: _allThreats.isEmpty ? null : _confirmClearAll,
        ),
        IconButton(
          icon: const Icon(LucideIcons.refresh_cw, color: AppColors.primary, size: 18),
          tooltip: 'Refresh',
          onPressed: _loadThreats,
        ),
      ],
      body: Column(
        children: [
          // Encrypted Storage Banner
          Padding(
            padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, 0),
            child: AppCard(
              backgroundColor: AppColors.surfaceAlt,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
              child: Row(
                children: [
                  const Icon(LucideIcons.lock, color: AppColors.safe, size: 16),
                  const SizedBox(width: AppSpacing.sm),
                  Text(
                    'Encrypted Local Storage (SQLite + FFI)',
                    style: AppTextStyles.labelSmall.copyWith(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    'Zero Cloud',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.textMuted),
                  ),
                ],
              ),
            ),
          ),

          // Search Input
          Padding(
            padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, 0),
            child: InputField(
              controller: _searchController,
              hintText: 'Search logged URLs, SMS, or reasons...',
              prefixIcon: const Icon(LucideIcons.search, size: 18, color: AppColors.textMuted),
              suffixIcon: _searchQuery.isNotEmpty
                  ? IconButton(
                      icon: const Icon(LucideIcons.x, size: 16, color: AppColors.textMuted),
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _searchQuery = '');
                      },
                    )
                  : null,
              onChanged: (val) {
                setState(() => _searchQuery = val.trim());
              },
            ),
          ),

          // Filter Chips
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
            child: Row(
              children: ['All', 'Blocked', 'Caution', 'Safe'].map((filter) {
                final isSelected = _selectedFilter == filter;
                return Padding(
                  padding: const EdgeInsets.only(right: AppSpacing.sm),
                  child: InkWell(
                    onTap: () {
                      setState(() {
                        _selectedFilter = filter;
                      });
                    },
                    borderRadius: AppRadius.badge,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.xs),
                      decoration: BoxDecoration(
                        color: isSelected ? AppColors.primaryLight : AppColors.surfaceAlt,
                        borderRadius: AppRadius.badge,
                        border: Border.all(
                          color: isSelected ? AppColors.primary : AppColors.border,
                          width: 1.0,
                        ),
                      ),
                      child: Text(
                        filter,
                        style: AppTextStyles.labelSmall.copyWith(
                          color: isSelected ? AppColors.primary : AppColors.textSecondary,
                          fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),

          // Log List
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
                : filtered.isEmpty
                    ? Center(
                        child: Padding(
                          padding: AppSpacing.paddingLg,
                          child: EmptyState(
                            icon: LucideIcons.list_clock,
                            title: _searchQuery.isNotEmpty
                                ? 'No records match "$_searchQuery"'
                                : 'Audit log is empty',
                            description: 'Inspected items will appear here automatically with local cryptographic audit trails.',
                          ),
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.xs),
                        itemCount: filtered.length,
                        itemBuilder: (ctx, idx) {
                          return ThreatListTile(threat: filtered[idx]);
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
